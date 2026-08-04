package com.tchalanet.server.app.ops;

import com.tchalanet.server.platform.ops.api.OpsResourceContributor;
import com.tchalanet.server.platform.ops.api.OpsServiceResourceItem;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Exposes archive-related table growth as actionable Ops dashboard guardrails. */
@Component
@Order(135)
public class ArchiveGrowthOpsResourceContributor implements OpsResourceContributor {

  private static final List<DatasetGuardrail> DATASETS =
      List.of(
          new DatasetGuardrail(
              "batch-history", "Spring Batch history", "batch", "c.relname ILIKE 'BATCH_%'"),
          new DatasetGuardrail(
              "sales-ticket-line",
              "Ticket lines",
              "public",
              "lower(c.relname) = 'sales_ticket_line'"),
          new DatasetGuardrail(
              "analytics",
              "Analytics projections",
              "public",
              "lower(c.relname) IN ('analytics_daily', 'analytics_draw', 'analytics_selection', 'analytics_seller_terminal_draw')"),
          new DatasetGuardrail(
              "audit-log",
              "Audit log partitions",
              "public",
              "lower(c.relname) = 'audit_log' OR lower(c.relname) LIKE 'audit_log_%'"),
          new DatasetGuardrail(
              "envers", "Envers audit tables", "public", "lower(c.relname) LIKE '%_aud'"));

  private final JdbcTemplate jdbc;
  private final boolean enabled;
  private final long warningRows;
  private final long criticalRows;
  private final int warningMb;
  private final int criticalMb;

  public ArchiveGrowthOpsResourceContributor(
      DataSource dataSource,
      @Value("${tch.archive.growth.enabled:true}") boolean enabled,
      @Value("${tch.archive.growth.warning-rows:1000000}") long warningRows,
      @Value("${tch.archive.growth.critical-rows:5000000}") long criticalRows,
      @Value("${tch.archive.growth.warning-mb:1024}") int warningMb,
      @Value("${tch.archive.growth.critical-mb:2048}") int criticalMb) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.enabled = enabled;
    this.warningRows = Math.max(1, warningRows);
    this.criticalRows = Math.max(this.warningRows + 1, criticalRows);
    this.warningMb = Math.max(1, warningMb);
    this.criticalMb = Math.max(this.warningMb + 1, criticalMb);
  }

  @Override
  public List<OpsServiceResourceItem> services() {
    if (!enabled) {
      return List.of();
    }

    return DATASETS.stream().map(this::measure).toList();
  }

  private OpsServiceResourceItem measure(DatasetGuardrail dataset) {
    try {
      var measurement =
          jdbc.queryForObject(
              dataset.measurementSql(),
              (rs, rowNum) ->
                  new Measurement(rs.getLong("row_count"), toMb(rs.getLong("size_bytes"))));
      if (measurement == null) {
        return unavailable(dataset, "No measurement returned.");
      }

      String severity = severity(measurement.rowCount(), measurement.sizeMb());
      String status =
          switch (severity) {
            case "CRITICAL" -> "CRITICAL";
            case "WARNING" -> "HIGH";
            default -> "OK";
          };
      return new OpsServiceResourceItem(
          "archive:growth:" + dataset.key(),
          dataset.displayName(),
          status,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          severity,
          message(dataset, measurement, severity),
          "/app/platform/ops/resources",
          measurement.sizeMb(),
          null,
          null);
    } catch (DataAccessException ex) {
      return unavailable(
          dataset, "Unable to measure dataset: " + ex.getMostSpecificCause().getMessage());
    }
  }

  private String severity(long rowCount, int sizeMb) {
    if (rowCount >= criticalRows || sizeMb >= criticalMb) return "CRITICAL";
    if (rowCount >= warningRows || sizeMb >= warningMb) return "WARNING";
    return "OK";
  }

  private String message(DatasetGuardrail dataset, Measurement measurement, String severity) {
    if ("CRITICAL".equals(severity)) {
      return "%s is above the critical archive guardrail: %,d rows and %d MB."
          .formatted(dataset.displayName(), measurement.rowCount(), measurement.sizeMb());
    }
    if ("WARNING".equals(severity)) {
      return "%s is approaching archive capacity: %,d rows and %d MB."
          .formatted(dataset.displayName(), measurement.rowCount(), measurement.sizeMb());
    }
    return "%s is within archive growth guardrails: %,d rows and %d MB."
        .formatted(dataset.displayName(), measurement.rowCount(), measurement.sizeMb());
  }

  private static OpsServiceResourceItem unavailable(DatasetGuardrail dataset, String reason) {
    return new OpsServiceResourceItem(
        "archive:growth:" + dataset.key(),
        dataset.displayName(),
        "UNKNOWN",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "WARNING",
        reason,
        "/app/platform/ops/resources",
        null,
        null,
        null);
  }

  private static int toMb(long bytes) {
    if (bytes <= 0) return 0;
    return (int) Math.max(1, Math.ceil(bytes / 1024.0 / 1024.0));
  }

  private record DatasetGuardrail(String key, String displayName, String schema, String predicate) {
    String measurementSql() {
      return """
          SELECT
            COALESCE(SUM(s.n_live_tup), 0) AS row_count,
            COALESCE(SUM(pg_total_relation_size(c.oid)), 0) AS size_bytes
          FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
          LEFT JOIN pg_stat_all_tables s ON s.relid = c.oid
          WHERE n.nspname = '%s'
            AND c.relkind IN ('r', 'p')
            AND (%s)
          """
          .formatted(schema, predicate);
    }
  }

  private record Measurement(long rowCount, int sizeMb) {}
}
