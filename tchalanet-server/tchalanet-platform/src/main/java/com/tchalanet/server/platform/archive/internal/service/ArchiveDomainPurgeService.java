package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLegalHoldJdbcRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveDomainPurgeService {

  private final ArchiveProperties props;
  private final NamedParameterJdbcTemplate jdbc;
  private final ArchiveLegalHoldJdbcRepository legalHoldRepo;

  public enum DomainPurgeDataset { DRAW, DRAW_RESULT, ENTITY_REVISION }

  public enum DomainPurgeMode { DRY_RUN, DELETE }

  public record DomainPurgePlan(
      DomainPurgeDataset dataset,
      UUID tenantId,
      LocalDate periodStart,
      LocalDate periodEnd,
      long hotRows,
      long archivedRows,
      long blockingRows,
      boolean eligible,
      String ineligibleReason
  ) {}

  public record DomainPurgeResult(
      DomainPurgeMode mode,
      DomainPurgePlan plan,
      long deletedChildRows,
      long deletedRows
  ) {}

  public DomainPurgePlan plan(DomainPurgeDataset dataset, UUID tenantId,
      LocalDate periodStart, LocalDate periodEnd) {
    validatePeriod(periodStart, periodEnd);
    String tableName = tableName(dataset);

    if (!periodEnd.isBefore(retentionCutoff())) {
      return notEligible(dataset, tenantId, periodStart, periodEnd,
          "period not older than retention cutoff " + retentionCutoff());
    }
    if (legalHoldRepo.hasActiveHoldForPeriod(tableName, periodStart, periodEnd)) {
      return notEligible(dataset, tenantId, periodStart, periodEnd,
          "active archive legal hold blocks purge for " + tableName);
    }

    long hotRows = hotCount(dataset, tenantId, periodStart, periodEnd);
    long archivedRows = archivedCount(tableName, tenantId, periodStart, periodEnd);
    long blockingRows = blockingCount(dataset, tenantId, periodStart, periodEnd);

    if (!archiveVerified(tableName, tenantId, periodStart, periodEnd)) {
      return new DomainPurgePlan(dataset, tenantId, periodStart, periodEnd,
          hotRows, archivedRows, blockingRows, false,
          "verified archive object is missing or invalid");
    }
    if (hotRows != archivedRows) {
      return new DomainPurgePlan(dataset, tenantId, periodStart, periodEnd,
          hotRows, archivedRows, blockingRows, false,
          "hot row count does not match verified archive row count");
    }
    if (blockingRows > 0) {
      return new DomainPurgePlan(dataset, tenantId, periodStart, periodEnd,
          hotRows, archivedRows, blockingRows, false,
          blockingReason(dataset));
    }

    return new DomainPurgePlan(dataset, tenantId, periodStart, periodEnd,
        hotRows, archivedRows, blockingRows, true, null);
  }

  @Transactional
  public DomainPurgeResult purge(DomainPurgeDataset dataset, UUID tenantId,
      LocalDate periodStart, LocalDate periodEnd, int batchSize,
      DomainPurgeMode mode, UUID requestedBy, String reason) {
    validateBatchSize(batchSize);
    if (reason == null || reason.trim().length() < 10) {
      throw new IllegalArgumentException("domain purge reason must be at least 10 characters");
    }

    DomainPurgePlan currentPlan = plan(dataset, tenantId, periodStart, periodEnd);
    if (!currentPlan.eligible()) {
      throw new IllegalStateException("domain purge refused: " + currentPlan.ineligibleReason());
    }
    if (mode == DomainPurgeMode.DRY_RUN) {
      log.info("archive domain purge dry-run: dataset={} tenant={} period={}/{} rows={} requestedBy={} reason={}",
          dataset, tenantId, periodStart, periodEnd, currentPlan.hotRows(), requestedBy, reason);
      return new DomainPurgeResult(mode, currentPlan, 0, 0);
    }
    if (!props.cleanup().enabled()) {
      throw new IllegalStateException("domain purge refused: tch.archive.cleanup.enabled is false");
    }

    Deletion deletion = switch (dataset) {
      case DRAW -> deleteDraws(tenantId, periodStart, periodEnd, batchSize);
      case DRAW_RESULT -> deleteDrawResults(periodStart, periodEnd, batchSize);
      case ENTITY_REVISION -> deleteEntityRevisions(periodStart, periodEnd, batchSize);
    };

    log.warn("archive domain purge executed: dataset={} tenant={} period={}/{} childRows={} rows={} requestedBy={} reason={}",
        dataset, tenantId, periodStart, periodEnd, deletion.childRows(), deletion.rows(),
        requestedBy, reason);
    return new DomainPurgeResult(mode, currentPlan, deletion.childRows(), deletion.rows());
  }

  private long hotCount(DomainPurgeDataset dataset, UUID tenantId,
      LocalDate periodStart, LocalDate periodEnd) {
    return switch (dataset) {
      case DRAW -> count("""
          SELECT COUNT(*)
            FROM draw
           WHERE scheduled_at >= :startAt
             AND scheduled_at <  :endAt
             AND deleted_at IS NULL
             AND (:tenantId IS NULL OR tenant_id = :tenantId)
          """, params(tenantId, periodStart, periodEnd));
      case DRAW_RESULT -> count("""
          SELECT COUNT(*)
            FROM draw_result
           WHERE occurred_at >= :startAt
             AND occurred_at <  :endAt
             AND deleted_at IS NULL
          """, params(null, periodStart, periodEnd));
      case ENTITY_REVISION -> count("""
          SELECT COUNT(*)
            FROM revinfo
           WHERE rev_timestamp >= :fromMillis
             AND rev_timestamp <  :toMillis
          """, revParams(periodStart, periodEnd));
    };
  }

  private long blockingCount(DomainPurgeDataset dataset, UUID tenantId,
      LocalDate periodStart, LocalDate periodEnd) {
    return switch (dataset) {
      case DRAW -> count("""
          SELECT COUNT(*)
            FROM sales_ticket t
            JOIN draw d ON d.id = t.draw_id
           WHERE d.scheduled_at >= :startAt
             AND d.scheduled_at <  :endAt
             AND (:tenantId IS NULL OR d.tenant_id = :tenantId)
          """, params(tenantId, periodStart, periodEnd));
      case DRAW_RESULT -> count("""
          SELECT COUNT(*)
            FROM draw d
            JOIN draw_result dr ON dr.id = d.draw_result_id
           WHERE dr.occurred_at >= :startAt
             AND dr.occurred_at <  :endAt
          """, params(null, periodStart, periodEnd));
      case ENTITY_REVISION -> 0;
    };
  }

  private Deletion deleteDraws(UUID tenantId, LocalDate periodStart, LocalDate periodEnd,
      int batchSize) {
    long analyticsSellerTerminal = deleteLoop("""
        WITH doomed AS (
          SELECT d.id
            FROM draw d
           WHERE d.scheduled_at >= :startAt
             AND d.scheduled_at <  :endAt
             AND (:tenantId IS NULL OR d.tenant_id = :tenantId)
           ORDER BY d.scheduled_at, d.id
           LIMIT :batchSize
        )
        DELETE FROM analytics_seller_terminal_draw a
         USING doomed
         WHERE a.draw_id = doomed.id
        """, params(tenantId, periodStart, periodEnd), batchSize);
    long analytics = deleteLoop("""
        WITH doomed AS (
          SELECT d.id
            FROM draw d
           WHERE d.scheduled_at >= :startAt
             AND d.scheduled_at <  :endAt
             AND (:tenantId IS NULL OR d.tenant_id = :tenantId)
           ORDER BY d.scheduled_at, d.id
           LIMIT :batchSize
        )
        DELETE FROM analytics_draw a
         USING doomed
         WHERE a.draw_id = doomed.id
        """, params(tenantId, periodStart, periodEnd), batchSize);
    long stats = deleteLoop("""
        WITH doomed AS (
          SELECT d.id
            FROM draw d
           WHERE d.scheduled_at >= :startAt
             AND d.scheduled_at <  :endAt
             AND (:tenantId IS NULL OR d.tenant_id = :tenantId)
           ORDER BY d.scheduled_at, d.id
           LIMIT :batchSize
        )
        DELETE FROM stats_draw s
         USING doomed
         WHERE s.draw_id = doomed.id
        """, params(tenantId, periodStart, periodEnd), batchSize);
    long draws = deleteLoop("""
        WITH doomed AS (
          SELECT d.id
            FROM draw d
           WHERE d.scheduled_at >= :startAt
             AND d.scheduled_at <  :endAt
             AND d.deleted_at IS NULL
             AND (:tenantId IS NULL OR d.tenant_id = :tenantId)
           ORDER BY d.scheduled_at, d.id
           LIMIT :batchSize
        )
        DELETE FROM draw d
         USING doomed
         WHERE d.id = doomed.id
        """, params(tenantId, periodStart, periodEnd), batchSize);
    return new Deletion(analyticsSellerTerminal + analytics + stats, draws);
  }

  private Deletion deleteDrawResults(LocalDate periodStart, LocalDate periodEnd, int batchSize) {
    long rows = deleteLoop("""
        WITH doomed AS (
          SELECT dr.id
            FROM draw_result dr
           WHERE dr.occurred_at >= :startAt
             AND dr.occurred_at <  :endAt
             AND dr.deleted_at IS NULL
           ORDER BY dr.occurred_at, dr.id
           LIMIT :batchSize
        )
        DELETE FROM draw_result dr
         USING doomed
         WHERE dr.id = doomed.id
        """, params(null, periodStart, periodEnd), batchSize);
    return new Deletion(0, rows);
  }

  private Deletion deleteEntityRevisions(LocalDate periodStart, LocalDate periodEnd, int batchSize) {
    MapSqlParameterSource params = revParams(periodStart, periodEnd);
    String doomed = """
        SELECT r.rev
          FROM revinfo r
         WHERE r.rev_timestamp >= :fromMillis
           AND r.rev_timestamp <  :toMillis
         ORDER BY r.rev
         LIMIT :batchSize
        """;
    long drawResultAud = deleteLoop("WITH doomed AS (" + doomed + ") DELETE FROM draw_result_aud a USING doomed WHERE a.rev = doomed.rev",
        params, batchSize);
    long limitAssignmentAud = deleteLoop("WITH doomed AS (" + doomed + ") DELETE FROM limit_assignment_aud a USING doomed WHERE a.rev = doomed.rev",
        params, batchSize);
    long sellerTerminalAud = deleteLoop("WITH doomed AS (" + doomed + ") DELETE FROM seller_terminal_aud a USING doomed WHERE a.rev = doomed.rev",
        params, batchSize);
    long revisions = deleteLoop("""
        WITH doomed AS (
          SELECT r.rev
            FROM revinfo r
           WHERE r.rev_timestamp >= :fromMillis
             AND r.rev_timestamp <  :toMillis
             AND NOT EXISTS (SELECT 1 FROM draw_result_aud a WHERE a.rev = r.rev)
             AND NOT EXISTS (SELECT 1 FROM limit_assignment_aud a WHERE a.rev = r.rev)
             AND NOT EXISTS (SELECT 1 FROM seller_terminal_aud a WHERE a.rev = r.rev)
           ORDER BY r.rev
           LIMIT :batchSize
        )
        DELETE FROM revinfo r
         USING doomed
         WHERE r.rev = doomed.rev
        """, params, batchSize);
    return new Deletion(drawResultAud + limitAssignmentAud + sellerTerminalAud, revisions);
  }

  private long archivedCount(String tableName, UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
    Long count = jdbc.queryForObject("""
        SELECT COALESCE(SUM(row_count), 0)
          FROM archive_object
         WHERE table_name = :table
           AND period_start = :periodStart
           AND period_end = :periodEnd
           AND status = 'VERIFIED'
           AND (:tenantId IS NULL OR tenant_id = :tenantId)
        """,
        new MapSqlParameterSource()
            .addValue("table", tableName)
            .addValue("periodStart", periodStart)
            .addValue("periodEnd", periodEnd)
            .addValue("tenantId", tenantId),
        Long.class);
    return count != null ? count : 0;
  }

  private boolean archiveVerified(String tableName, UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
    long verified = count("""
        SELECT COUNT(*)
          FROM archive_object
         WHERE table_name = :table
           AND period_start = :periodStart
           AND period_end = :periodEnd
           AND status = 'VERIFIED'
           AND (:tenantId IS NULL OR tenant_id = :tenantId)
        """,
        new MapSqlParameterSource()
            .addValue("table", tableName)
            .addValue("periodStart", periodStart)
            .addValue("periodEnd", periodEnd)
            .addValue("tenantId", tenantId));
    long invalid = count("""
        SELECT COUNT(*)
          FROM archive_object
         WHERE table_name = :table
           AND period_start = :periodStart
           AND period_end = :periodEnd
           AND status = 'INVALID'
           AND (:tenantId IS NULL OR tenant_id = :tenantId)
        """,
        new MapSqlParameterSource()
            .addValue("table", tableName)
            .addValue("periodStart", periodStart)
            .addValue("periodEnd", periodEnd)
            .addValue("tenantId", tenantId));
    return verified > 0 && invalid == 0;
  }

  private long deleteLoop(String sql, MapSqlParameterSource params, int batchSize) {
    long total = 0;
    int deleted;
    do {
      deleted = jdbc.update(sql, copy(params).addValue("batchSize", batchSize));
      total += deleted;
    } while (deleted == batchSize);
    return total;
  }

  private long count(String sql, MapSqlParameterSource params) {
    Long count = jdbc.queryForObject(sql, params, Long.class);
    return count != null ? count : 0;
  }

  private DomainPurgePlan notEligible(DomainPurgeDataset dataset, UUID tenantId,
      LocalDate periodStart, LocalDate periodEnd, String reason) {
    return new DomainPurgePlan(dataset, tenantId, periodStart, periodEnd,
        hotCount(dataset, tenantId, periodStart, periodEnd),
        archivedCount(tableName(dataset), tenantId, periodStart, periodEnd),
        blockingCount(dataset, tenantId, periodStart, periodEnd),
        false, reason);
  }

  private static String tableName(DomainPurgeDataset dataset) {
    return switch (dataset) {
      case DRAW -> "draw";
      case DRAW_RESULT -> "draw_result";
      case ENTITY_REVISION -> "entity_revision";
    };
  }

  private static String blockingReason(DomainPurgeDataset dataset) {
    return switch (dataset) {
      case DRAW -> "draw purge refused while tickets still reference matching draws";
      case DRAW_RESULT -> "draw_result purge refused while draws still reference matching results";
      case ENTITY_REVISION -> "entity revision purge blocked";
    };
  }

  private LocalDate retentionCutoff() {
    return LocalDate.now(ZoneOffset.UTC).minusMonths(props.cleanup().retentionMonths());
  }

  private static MapSqlParameterSource params(UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
    return new MapSqlParameterSource()
        .addValue("tenantId", tenantId)
        .addValue("startAt", periodStart.atStartOfDay().toInstant(ZoneOffset.UTC))
        .addValue("endAt", periodEnd.atStartOfDay().toInstant(ZoneOffset.UTC));
  }

  private static MapSqlParameterSource revParams(LocalDate periodStart, LocalDate periodEnd) {
    return new MapSqlParameterSource()
        .addValue("fromMillis", periodStart.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        .addValue("toMillis", periodEnd.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
  }

  private static MapSqlParameterSource copy(MapSqlParameterSource source) {
    MapSqlParameterSource copy = new MapSqlParameterSource();
    for (String name : source.getParameterNames()) {
      copy.addValue(name, source.getValue(name));
    }
    return copy;
  }

  private static void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
    if (periodStart == null || periodEnd == null || !periodStart.isBefore(periodEnd)) {
      throw new IllegalArgumentException("domain purge period must have periodStart < periodEnd");
    }
  }

  private static void validateBatchSize(int batchSize) {
    if (batchSize < 1 || batchSize > 100_000) {
      throw new IllegalArgumentException("domain purge batchSize must be between 1 and 100000");
    }
  }

  private record Deletion(long childRows, long rows) {}
}
