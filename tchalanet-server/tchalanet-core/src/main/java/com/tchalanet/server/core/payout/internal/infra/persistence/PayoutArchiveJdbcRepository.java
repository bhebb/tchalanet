package com.tchalanet.server.core.payout.internal.infra.persistence;

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
 * JDBC operations on {@code payout} used exclusively by the archive system.
 *
 * <p>Streaming avoids loading full months into memory. Soft-deleted payouts
 * ({@code deleted_at IS NOT NULL}) are excluded per retention policy.
 */
@Repository
@RequiredArgsConstructor
public class PayoutArchiveJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  /** Count payouts created in [from, to) for the given tenant (null = all tenants). */
  public long countByPeriod(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT COUNT(*) FROM payout
         WHERE created_at >= :from AND created_at < :to
           AND deleted_at IS NULL
        """ + tenantClause;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to",   Timestamp.from(to));
    if (tenantId != null) params.addValue("tenantId", tenantId);

    Long count = jdbc.queryForObject(sql, params, Long.class);
    return count != null ? count : 0L;
  }

  /**
   * Stream all non-deleted payout rows in [from, to) for the given tenant.
   * Rows are emitted in {@code created_at ASC} order.
   * Timestamps → {@link Instant}.
   */
  public void streamByPeriod(Instant from, Instant to, UUID tenantId,
      Consumer<Map<String, Object>> consumer) {

    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT id, tenant_id, ticket_id, draw_id,
               amount_cents, currency, status, source, source_event_id,
               selling_outlet_id, selling_session_id, opened_at,
               paying_outlet_id, paying_session_id, paying_terminal_id,
               paid_by, paid_at,
               blocked_by, blocked_at, block_reason,
               cancelled_by, cancelled_at, cancel_reason,
               reversed_by, reversed_at, reverse_reason,
               created_at, updated_at, created_by, updated_by
          FROM payout
         WHERE created_at >= :from AND created_at < :to
           AND deleted_at IS NULL
        """ + tenantClause + """
         ORDER BY created_at
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
          row.put(meta.getColumnLabel(i), val);
        }
        consumer.accept(row);
      }
      return null;
    });
  }

  /**
   * Return one lookup-index row per payout for the given period.
   * Used by {@code generateLookupRows()} in the archive provider.
   */
  public List<Map<String, Object>> findLookupRows(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT id, tenant_id, created_at
          FROM payout
         WHERE created_at >= :from AND created_at < :to
           AND deleted_at IS NULL
        """ + tenantClause + """
         ORDER BY created_at
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to",   Timestamp.from(to));
    if (tenantId != null) params.addValue("tenantId", tenantId);

    List<Map<String, Object>> results = new ArrayList<>();
    jdbc.query(sql, params, rs -> {
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id",        rs.getObject("id",        UUID.class));
        row.put("tenant_id", rs.getObject("tenant_id", UUID.class));
        Timestamp ts = rs.getTimestamp("created_at");
        row.put("created_at", ts != null ? ts.toInstant() : null);
        results.add(row);
      }
      return null;
    });
    return results;
  }
}
