package com.tchalanet.server.core.draw.internal.infra.persistence;

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

/** JDBC archive reads for {@code draw}, bounded by {@code scheduled_at}. */
@Repository
@RequiredArgsConstructor
public class DrawArchiveJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public long countByScheduledPeriod(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT COUNT(*)
          FROM draw
         WHERE scheduled_at >= :from AND scheduled_at < :to
           AND deleted_at IS NULL
        """ + tenantClause;

    MapSqlParameterSource params = params(from, to, tenantId);
    Long count = jdbc.queryForObject(sql, params, Long.class);
    return count != null ? count : 0L;
  }

  public void streamByScheduledPeriod(
      Instant from, Instant to, UUID tenantId, Consumer<Map<String, Object>> consumer) {

    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT id, tenant_id, draw_channel_id, draw_date, scheduled_at, cutoff_at,
               opened_at, closed_at, resulted_at, settled_at, canceled_at,
               cancel_reason_code, cancel_reason_label, status, draw_result_id,
               system_generated, locked, result_source, result_override_reason,
               result_overridden_at, created_at, created_by, updated_at, updated_by,
               version
          FROM draw
         WHERE scheduled_at >= :from AND scheduled_at < :to
           AND deleted_at IS NULL
        """ + tenantClause + """
         ORDER BY scheduled_at, tenant_id, id
        """;

    jdbc.query(sql, params(from, to, tenantId), rs -> {
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

  public List<Map<String, Object>> findLookupRows(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT id, tenant_id, draw_date, scheduled_at
          FROM draw
         WHERE scheduled_at >= :from AND scheduled_at < :to
           AND deleted_at IS NULL
        """ + tenantClause + """
         ORDER BY scheduled_at, tenant_id, id
        """;

    List<Map<String, Object>> results = new ArrayList<>();
    jdbc.query(sql, params(from, to, tenantId), rs -> {
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getObject("id", UUID.class));
        row.put("tenant_id", rs.getObject("tenant_id", UUID.class));
        java.sql.Date drawDate = rs.getDate("draw_date");
        row.put("draw_date", drawDate != null ? drawDate.toLocalDate() : null);
        Timestamp scheduledAt = rs.getTimestamp("scheduled_at");
        row.put("scheduled_at", scheduledAt != null ? scheduledAt.toInstant() : null);
        results.add(row);
      }
      return null;
    });
    return results;
  }

  private MapSqlParameterSource params(Instant from, Instant to, UUID tenantId) {
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to", Timestamp.from(to));
    if (tenantId != null) {
      params.addValue("tenantId", tenantId);
    }
    return params;
  }
}
