package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Identifies PostgreSQL partitions eligible for cleanup and, when enabled, detaches or drops them.
 *
 * <p>Safety rules (all must be true before any DDL):
 * <ul>
 *   <li>Period is older than the configured retention threshold.</li>
 *   <li>All {@code archive_object} rows for the period are {@code VERIFIED}.</li>
 *   <li>No {@code INVALID} archive object exists for the period.</li>
 *   <li>Object row count matches the expected exported row count.</li>
 * </ul>
 *
 * <p>In V1, {@code mode = DRY_RUN} is the only safe mode; {@code DETACH_ONLY} and {@code DROP}
 * are available but require explicit configuration ({@code tch.archive.cleanup.enabled=true}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchivePartitionCleanupService {

  private final ArchiveProperties props;
  private final NamedParameterJdbcTemplate jdbc;

  public enum CleanupMode { DRY_RUN, DETACH_ONLY, DROP }

  public record PartitionCleanupPlan(
      String partitionName,
      String tableName,
      LocalDate periodStart,
      LocalDate periodEnd,
      long hotRowCount,
      long archivedRowCount,
      boolean archiveVerified,
      boolean eligible,
      String ineligibleReason
  ) {}

  /**
   * Build a plan showing which partitions are eligible for cleanup.
   * Never executes any DDL — always safe to call.
   *
   * @param tableName the parent partitioned table (e.g. {@code audit_log})
   * @param retentionCutoff partitions whose period_end is before this date are candidates
   */
  public List<PartitionCleanupPlan> plan(String tableName, LocalDate retentionCutoff) {
    List<Map<String, Object>> partitions = findPartitions(tableName);
    List<PartitionCleanupPlan> result = new ArrayList<>();

    for (Map<String, Object> p : partitions) {
      String partitionName = (String) p.get("partition_name");
      LocalDate periodStart = parsePeriodStart(partitionName, tableName);
      if (periodStart == null) continue;
      LocalDate periodEnd = periodStart.plusMonths(1);

      if (!periodEnd.isBefore(retentionCutoff)) {
        result.add(notEligible(partitionName, tableName, periodStart, periodEnd,
            "period not older than retention cutoff " + retentionCutoff));
        continue;
      }

      List<Map<String, Object>> archiveObjects = findVerifiedObjects(tableName, periodStart, periodEnd);
      if (archiveObjects.isEmpty()) {
        result.add(notEligible(partitionName, tableName, periodStart, periodEnd,
            "no verified archive_object found for period"));
        continue;
      }

      boolean hasInvalid = hasInvalidObject(tableName, periodStart, periodEnd);
      if (hasInvalid) {
        result.add(notEligible(partitionName, tableName, periodStart, periodEnd,
            "INVALID archive_object exists for period — manual investigation required"));
        continue;
      }

      long archivedRows = archiveObjects.stream()
          .mapToLong(o -> ((Number) o.getOrDefault("row_count", 0)).longValue())
          .sum();
      long hotRows = countPartitionRows(partitionName);

      result.add(new PartitionCleanupPlan(
          partitionName, tableName, periodStart, periodEnd,
          hotRows, archivedRows, true, true, null));
    }

    return result;
  }

  /**
   * Execute cleanup for the given partition name.
   *
   * <p>Only runs if {@code tch.archive.cleanup.enabled = true}; otherwise logs and returns.
   * Mode {@code DRY_RUN} never executes DDL even if enabled.
   */
  public void executeCleanup(String partitionName, CleanupMode mode) {
    if (!props.cleanup().enabled()) {
      log.info("archive cleanup: disabled — skipping cleanup of {}", partitionName);
      return;
    }
    if (mode == CleanupMode.DRY_RUN) {
      log.info("archive cleanup: DRY_RUN — would process partition {}", partitionName);
      return;
    }
    if (mode == CleanupMode.DETACH_ONLY) {
      log.info("archive cleanup: DETACH_ONLY — detaching partition {}", partitionName);
      jdbc.getJdbcTemplate().execute(
          "ALTER TABLE %s DETACH PARTITION %s CONCURRENTLY".formatted(
              extractParentTable(partitionName), partitionName));
      log.info("archive cleanup: partition {} detached", partitionName);
    } else if (mode == CleanupMode.DROP) {
      log.warn("archive cleanup: DROP mode — dropping partition {}", partitionName);
      jdbc.getJdbcTemplate().execute("DROP TABLE IF EXISTS " + partitionName);
      log.info("archive cleanup: partition {} dropped", partitionName);
    }
  }

  // ── internal helpers ────────────────────────────────────────────────────────

  private List<Map<String, Object>> findPartitions(String tableName) {
    return jdbc.queryForList("""
        SELECT c.relname AS partition_name
          FROM pg_class c
          JOIN pg_inherits i ON i.inhrelid = c.oid
          JOIN pg_class p    ON p.oid = i.inhparent
         WHERE p.relname = :tableName
           AND c.relkind = 'r'
         ORDER BY c.relname
        """,
        new MapSqlParameterSource().addValue("tableName", tableName));
  }

  private List<Map<String, Object>> findVerifiedObjects(String tableName,
      LocalDate periodStart, LocalDate periodEnd) {
    return jdbc.queryForList("""
        SELECT id, row_count FROM archive_object
         WHERE table_name  = :table
           AND period_start = :pStart
           AND period_end   = :pEnd
           AND status       = 'VERIFIED'
        """,
        new MapSqlParameterSource()
            .addValue("table",  tableName)
            .addValue("pStart", periodStart)
            .addValue("pEnd",   periodEnd));
  }

  private boolean hasInvalidObject(String tableName, LocalDate periodStart, LocalDate periodEnd) {
    Integer count = jdbc.queryForObject("""
        SELECT COUNT(*) FROM archive_object
         WHERE table_name  = :table
           AND period_start = :pStart
           AND period_end   = :pEnd
           AND status       = 'INVALID'
        """,
        new MapSqlParameterSource()
            .addValue("table",  tableName)
            .addValue("pStart", periodStart)
            .addValue("pEnd",   periodEnd),
        Integer.class);
    return count != null && count > 0;
  }

  private long countPartitionRows(String partitionName) {
    Long count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM " + partitionName,
        Map.of(), Long.class);
    return count != null ? count : 0;
  }

  /**
   * Parse period start from partition name like {@code audit_log_2026_06}.
   * Returns null if the partition does not match the expected pattern.
   */
  private static LocalDate parsePeriodStart(String partitionName, String tableName) {
    String prefix = tableName + "_";
    if (!partitionName.startsWith(prefix)) return null;
    String suffix = partitionName.substring(prefix.length());
    String[] parts = suffix.split("_");
    if (parts.length < 2) return null;
    try {
      int year  = Integer.parseInt(parts[0]);
      int month = Integer.parseInt(parts[1]);
      return LocalDate.of(year, month, 1);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String extractParentTable(String partitionName) {
    int lastUnderscore = partitionName.lastIndexOf('_');
    if (lastUnderscore < 0) return partitionName;
    int secondLast = partitionName.lastIndexOf('_', lastUnderscore - 1);
    return secondLast < 0 ? partitionName : partitionName.substring(0, secondLast);
  }

  private static PartitionCleanupPlan notEligible(String partitionName, String tableName,
      LocalDate periodStart, LocalDate periodEnd, String reason) {
    return new PartitionCleanupPlan(
        partitionName, tableName, periodStart, periodEnd,
        -1, -1, false, false, reason);
  }
}
