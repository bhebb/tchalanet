package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLegalHoldJdbcRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges archived ticket rows from hot storage after archive verification.
 *
 * <p>This is intentionally not partition DDL. It performs bounded deletes for the
 * current sales ticket schema, where ticket children reference {@code sales_ticket(id)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveTicketPurgeService {

  private static final List<String> DATASETS = List.of(
      "sales_ticket_charge",
      "sales_ticket_line",
      "sales_ticket");

  private final ArchiveProperties props;
  private final NamedParameterJdbcTemplate jdbc;
  private final ArchiveLegalHoldJdbcRepository legalHoldRepo;

  public enum TicketPurgeMode { DRY_RUN, DELETE }

  public record TicketPurgePlan(
      UUID tenantId,
      LocalDate periodStart,
      LocalDate periodEnd,
      long hotTickets,
      long hotLines,
      long hotCharges,
      long archivedTickets,
      long archivedLines,
      long archivedCharges,
      boolean eligible,
      String ineligibleReason
  ) {}

  public record TicketPurgeResult(
      TicketPurgeMode mode,
      TicketPurgePlan plan,
      long deletedCharges,
      long deletedLines,
      long deletedTickets
  ) {}

  public TicketPurgePlan plan(UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
    validatePeriod(periodStart, periodEnd);

    if (!periodEnd.isBefore(retentionCutoff())) {
      return notEligible(tenantId, periodStart, periodEnd,
          "period not older than retention cutoff " + retentionCutoff());
    }

    for (String dataset : DATASETS) {
      if (legalHoldRepo.hasActiveHoldForPeriod(dataset, periodStart, periodEnd)) {
        return notEligible(tenantId, periodStart, periodEnd,
            "active archive legal hold blocks ticket purge for " + dataset);
      }
    }

    Counts hot = hotCounts(tenantId, periodStart, periodEnd);
    Counts archived = archivedCounts(tenantId, periodStart, periodEnd);

    if (!archiveVerified(tenantId, periodStart, periodEnd)) {
      return new TicketPurgePlan(tenantId, periodStart, periodEnd,
          hot.tickets(), hot.lines(), hot.charges(),
          archived.tickets(), archived.lines(), archived.charges(),
          false, "verified archive objects are missing or invalid");
    }

    if (!hot.equals(archived)) {
      return new TicketPurgePlan(tenantId, periodStart, periodEnd,
          hot.tickets(), hot.lines(), hot.charges(),
          archived.tickets(), archived.lines(), archived.charges(),
          false, "hot row counts do not match verified archive row counts");
    }

    return new TicketPurgePlan(tenantId, periodStart, periodEnd,
        hot.tickets(), hot.lines(), hot.charges(),
        archived.tickets(), archived.lines(), archived.charges(),
        true, null);
  }

  @Transactional
  public TicketPurgeResult purge(UUID tenantId, LocalDate periodStart, LocalDate periodEnd,
      int batchSize, TicketPurgeMode mode, UUID requestedBy, String reason) {
    validateBatchSize(batchSize);
    if (reason == null || reason.trim().length() < 10) {
      throw new IllegalArgumentException("ticket purge reason must be at least 10 characters");
    }

    TicketPurgePlan currentPlan = plan(tenantId, periodStart, periodEnd);
    if (!currentPlan.eligible()) {
      throw new IllegalStateException("ticket purge refused: " + currentPlan.ineligibleReason());
    }
    if (mode == TicketPurgeMode.DRY_RUN) {
      log.info("archive ticket purge dry-run: tenant={} period={}/{} tickets={} lines={} charges={} requestedBy={} reason={}",
          tenantId, periodStart, periodEnd,
          currentPlan.hotTickets(), currentPlan.hotLines(), currentPlan.hotCharges(),
          requestedBy, reason);
      return new TicketPurgeResult(mode, currentPlan, 0, 0, 0);
    }
    if (!props.cleanup().enabled()) {
      throw new IllegalStateException("ticket purge refused: tch.archive.cleanup.enabled is false");
    }

    long deletedCharges = deleteCharges(tenantId, periodStart, periodEnd, batchSize);
    long deletedLines = deleteLines(tenantId, periodStart, periodEnd, batchSize);
    long deletedTickets = deleteTickets(tenantId, periodStart, periodEnd, batchSize);

    log.warn("archive ticket purge executed: tenant={} period={}/{} charges={} lines={} tickets={} requestedBy={} reason={}",
        tenantId, periodStart, periodEnd, deletedCharges, deletedLines, deletedTickets,
        requestedBy, reason);

    return new TicketPurgeResult(mode, currentPlan, deletedCharges, deletedLines, deletedTickets);
  }

  private Counts hotCounts(UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
    MapSqlParameterSource params = params(tenantId, periodStart, periodEnd);
    long tickets = count("""
        SELECT COUNT(*)
          FROM sales_ticket t
         WHERE t.sold_at >= :startAt
           AND t.sold_at <  :endAt
           AND (:tenantId IS NULL OR t.tenant_id = :tenantId)
        """, params);
    long lines = count("""
        SELECT COUNT(*)
          FROM sales_ticket_line tl
          JOIN sales_ticket t ON t.id = tl.ticket_id
         WHERE t.sold_at >= :startAt
           AND t.sold_at <  :endAt
           AND (:tenantId IS NULL OR t.tenant_id = :tenantId)
        """, params);
    long charges = count("""
        SELECT COUNT(*)
          FROM sales_ticket_charge c
          JOIN sales_ticket t ON t.id = c.sales_ticket_id
         WHERE t.sold_at >= :startAt
           AND t.sold_at <  :endAt
           AND (:tenantId IS NULL OR t.tenant_id = :tenantId)
        """, params);
    return new Counts(tickets, lines, charges);
  }

  private Counts archivedCounts(UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
    return new Counts(
        archivedCount("sales_ticket", tenantId, periodStart, periodEnd),
        archivedCount("sales_ticket_line", tenantId, periodStart, periodEnd),
        archivedCount("sales_ticket_charge", tenantId, periodStart, periodEnd));
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

  private boolean archiveVerified(UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
    for (String dataset : DATASETS) {
      long objectCount = count("""
          SELECT COUNT(*)
            FROM archive_object
           WHERE table_name = :table
             AND period_start = :periodStart
             AND period_end = :periodEnd
             AND status = 'VERIFIED'
             AND (:tenantId IS NULL OR tenant_id = :tenantId)
          """,
          new MapSqlParameterSource()
              .addValue("table", dataset)
              .addValue("periodStart", periodStart)
              .addValue("periodEnd", periodEnd)
              .addValue("tenantId", tenantId));
      if (objectCount == 0) {
        return false;
      }
    }

    long invalidObjects = count("""
        SELECT COUNT(*)
          FROM archive_object
         WHERE table_name IN (:datasets)
           AND period_start = :periodStart
           AND period_end = :periodEnd
           AND status = 'INVALID'
           AND (:tenantId IS NULL OR tenant_id = :tenantId)
        """,
        new MapSqlParameterSource()
            .addValue("datasets", DATASETS)
            .addValue("periodStart", periodStart)
            .addValue("periodEnd", periodEnd)
            .addValue("tenantId", tenantId));
    return invalidObjects == 0;
  }

  private long deleteCharges(UUID tenantId, LocalDate periodStart, LocalDate periodEnd, int batchSize) {
    return deleteLoop("""
        WITH doomed AS (
          SELECT c.id
            FROM sales_ticket_charge c
            JOIN sales_ticket t ON t.id = c.sales_ticket_id
           WHERE t.sold_at >= :startAt
             AND t.sold_at <  :endAt
             AND (:tenantId IS NULL OR t.tenant_id = :tenantId)
           ORDER BY c.id
           LIMIT :batchSize
        )
        DELETE FROM sales_ticket_charge c
         USING doomed
         WHERE c.id = doomed.id
        """, tenantId, periodStart, periodEnd, batchSize);
  }

  private long deleteLines(UUID tenantId, LocalDate periodStart, LocalDate periodEnd, int batchSize) {
    return deleteLoop("""
        WITH doomed AS (
          SELECT tl.id
            FROM sales_ticket_line tl
            JOIN sales_ticket t ON t.id = tl.ticket_id
           WHERE t.sold_at >= :startAt
             AND t.sold_at <  :endAt
             AND (:tenantId IS NULL OR t.tenant_id = :tenantId)
           ORDER BY tl.id
           LIMIT :batchSize
        )
        DELETE FROM sales_ticket_line tl
         USING doomed
         WHERE tl.id = doomed.id
        """, tenantId, periodStart, periodEnd, batchSize);
  }

  private long deleteTickets(UUID tenantId, LocalDate periodStart, LocalDate periodEnd, int batchSize) {
    return deleteLoop("""
        WITH doomed AS (
          SELECT t.id
            FROM sales_ticket t
           WHERE t.sold_at >= :startAt
             AND t.sold_at <  :endAt
             AND (:tenantId IS NULL OR t.tenant_id = :tenantId)
           ORDER BY t.id
           LIMIT :batchSize
        )
        DELETE FROM sales_ticket t
         USING doomed
         WHERE t.id = doomed.id
        """, tenantId, periodStart, periodEnd, batchSize);
  }

  private long deleteLoop(String sql, UUID tenantId, LocalDate periodStart, LocalDate periodEnd, int batchSize) {
    long total = 0;
    int deleted;
    do {
      deleted = jdbc.update(sql, params(tenantId, periodStart, periodEnd).addValue("batchSize", batchSize));
      total += deleted;
    } while (deleted == batchSize);
    return total;
  }

  private long count(String sql, MapSqlParameterSource params) {
    Long count = jdbc.queryForObject(sql, params, Long.class);
    return count != null ? count : 0;
  }

  private TicketPurgePlan notEligible(UUID tenantId, LocalDate periodStart, LocalDate periodEnd,
      String reason) {
    Counts hot = hotCounts(tenantId, periodStart, periodEnd);
    Counts archived = archivedCounts(tenantId, periodStart, periodEnd);
    return new TicketPurgePlan(tenantId, periodStart, periodEnd,
        hot.tickets(), hot.lines(), hot.charges(),
        archived.tickets(), archived.lines(), archived.charges(),
        false, reason);
  }

  private MapSqlParameterSource params(UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
    return new MapSqlParameterSource()
        .addValue("tenantId", tenantId)
        .addValue("startAt", periodStart.atStartOfDay().toInstant(ZoneOffset.UTC))
        .addValue("endAt", periodEnd.atStartOfDay().toInstant(ZoneOffset.UTC));
  }

  private LocalDate retentionCutoff() {
    return LocalDate.now(ZoneOffset.UTC).minusMonths(props.cleanup().retentionMonths());
  }

  private static void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
    if (periodStart == null || periodEnd == null || !periodStart.isBefore(periodEnd)) {
      throw new IllegalArgumentException("ticket purge period must have periodStart < periodEnd");
    }
  }

  private static void validateBatchSize(int batchSize) {
    if (batchSize < 1 || batchSize > 100_000) {
      throw new IllegalArgumentException("ticket purge batchSize must be between 1 and 100000");
    }
  }

  private record Counts(long tickets, long lines, long charges) {}
}
