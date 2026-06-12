package com.tchalanet.server.platform.audit.internal.persistence;

import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC operations on the partitioned {@code audit_log} table.
 *
 * <p>Used by {@code AuditLogArchiveDatasetProvider} to plan and stream rows during
 * archive export. Separate from {@code AuditEventRepositoryAdapter} which targets the
 * legacy {@code audit_event} table.
 */
@Repository
@RequiredArgsConstructor
public class AuditLogJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  /** Count rows in [from, to) for the given tenant (null = all tenants). */
  public long countByPeriod(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT COUNT(*) FROM audit_log
         WHERE occurred_at >= :from AND occurred_at < :to
        """ + tenantClause;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to",   Timestamp.from(to));
    if (tenantId != null) params.addValue("tenantId", tenantId);

    Long count = jdbc.queryForObject(sql, params, Long.class);
    return count != null ? count : 0L;
  }

  /**
   * Stream all rows in [from, to) for the given tenant, invoking {@code consumer} per row.
   *
   * <p>Rows are emitted in {@code occurred_at ASC} order. Timestamps are converted to
   * {@link Instant}; dates to {@link java.time.LocalDate} for clean JSON serialization.
   */
  public void streamByPeriod(Instant from, Instant to, UUID tenantId,
      Consumer<Map<String, Object>> consumer) {

    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT * FROM audit_log
         WHERE occurred_at >= :from AND occurred_at < :to
        """ + tenantClause + """
         ORDER BY occurred_at
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to",   Timestamp.from(to));
    if (tenantId != null) params.addValue("tenantId", tenantId);

    jdbc.query(sql, params, rs -> {
      ResultSetMetaData meta = rs.getMetaData();
      int cols = meta.getColumnCount();
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>(cols * 2);
        for (int i = 1; i <= cols; i++) {
          Object val = rs.getObject(i);
          if (val instanceof Timestamp ts) val = ts.toInstant();
          else if (val instanceof java.sql.Date d) val = d.toLocalDate();
          row.put(meta.getColumnLabel(i), val);
        }
        consumer.accept(row);
      }
      return null;
    });
  }

  /**
   * Return one representative row per distinct (tenant_id, entity_type, entity_id) group
   * within [from, to). Used to build the coarse-grained archive lookup index.
   *
   * <p>Rows with a null entity_id are excluded (e.g. global lifecycle events).
   */
  public List<Map<String, Object>> findDistinctEntityLookupRows(
      Instant from, Instant to, UUID tenantId, UUID archiveObjectId) {

    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT tenant_id, entity_type, entity_id, MIN(occurred_at) AS occurred_at
          FROM audit_log
         WHERE occurred_at >= :from AND occurred_at < :to
           AND entity_id IS NOT NULL
        """ + tenantClause + """
         GROUP BY tenant_id, entity_type, entity_id
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to",   Timestamp.from(to));
    if (tenantId != null) params.addValue("tenantId", tenantId);

    List<Map<String, Object>> results = new ArrayList<>();
    jdbc.query(sql, params, rs -> {
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tenant_id",   rs.getObject("tenant_id",  UUID.class));
        row.put("entity_type", rs.getString("entity_type"));
        row.put("entity_id",   rs.getObject("entity_id",  UUID.class));
        Timestamp ts = rs.getTimestamp("occurred_at");
        row.put("occurred_at", ts != null ? ts.toInstant() : null);
        results.add(row);
      }
      return null;
    });
    return results;
  }
}
