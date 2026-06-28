package com.tchalanet.server.core.drawresult.internal.infra.persistence;

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

/** JDBC archive reads for global {@code draw_result}, bounded by {@code occurred_at}. */
@Repository
@RequiredArgsConstructor
public class DrawResultArchiveJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public long countByOccurredPeriod(Instant from, Instant to) {
    String sql = """
        SELECT COUNT(*)
          FROM draw_result
         WHERE occurred_at >= :from AND occurred_at < :to
           AND deleted_at IS NULL
        """;

    Long count = jdbc.queryForObject(sql, params(from, to), Long.class);
    return count != null ? count : 0L;
  }

  public void streamByOccurredPeriod(
      Instant from, Instant to, Consumer<Map<String, Object>> consumer) {

    String sql = """
        SELECT id, result_slot_id, occurred_at, result_date, source_result,
               haiti_result, raw_payload, flags, status, quality, source, source_hash,
               fetched_at, override_reason, created_at, created_by, updated_at, updated_by,
               version
          FROM draw_result
         WHERE occurred_at >= :from AND occurred_at < :to
           AND deleted_at IS NULL
         ORDER BY occurred_at, result_slot_id, id
        """;

    jdbc.query(sql, params(from, to), rs -> {
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

  public List<Map<String, Object>> findLookupRows(Instant from, Instant to) {
    String sql = """
        SELECT id, result_date, occurred_at, source_hash
          FROM draw_result
         WHERE occurred_at >= :from AND occurred_at < :to
           AND deleted_at IS NULL
         ORDER BY occurred_at, result_slot_id, id
        """;

    List<Map<String, Object>> results = new ArrayList<>();
    jdbc.query(sql, params(from, to), rs -> {
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getObject("id", UUID.class));
        java.sql.Date resultDate = rs.getDate("result_date");
        row.put("result_date", resultDate != null ? resultDate.toLocalDate() : null);
        Timestamp occurredAt = rs.getTimestamp("occurred_at");
        row.put("occurred_at", occurredAt != null ? occurredAt.toInstant() : null);
        row.put("source_hash", rs.getString("source_hash"));
        results.add(row);
      }
      return null;
    });
    return results;
  }

  private MapSqlParameterSource params(Instant from, Instant to) {
    return new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to", Timestamp.from(to));
  }
}
