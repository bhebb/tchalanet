package com.tchalanet.server.core.sales.internal.infra.persistence;

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
 * JDBC operations on {@code sales_ticket} used exclusively by the archive system.
 *
 * <p>Streaming is done via {@code ResultSetExtractor} to avoid loading full months into memory.
 * Soft-deleted tickets ({@code deleted_at IS NOT NULL}) are excluded — they are preserved in
 * the hot table indefinitely as a matter of policy.
 */
@Repository
@RequiredArgsConstructor
public class TicketArchiveJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  /** Count tickets sold in [from, to) for the given tenant (null = all tenants). */
  public long countByPeriod(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT COUNT(*) FROM sales_ticket
         WHERE sold_at >= :from AND sold_at < :to
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
   * Stream all non-deleted ticket rows in [from, to) for the given tenant.
   * Rows are emitted in {@code sold_at ASC} order.
   * Timestamps → {@link Instant}, dates → {@link java.time.LocalDate}.
   */
  public void streamByPeriod(Instant from, Instant to, UUID tenantId,
      Consumer<Map<String, Object>> consumer) {

    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT id, tenant_id, public_code, ticket_code, sale_status, result_status,
               settlement_status, sold_at, placed_at, currency,
               stake_amount, total_amount, potential_payout_amount, winning_amount,
               seller_terminal_id, draw_id, draw_channel_id, sale_channel,
               cancelled_at, voided_at, paid_at, created_at
          FROM sales_ticket
         WHERE sold_at >= :from AND sold_at < :to
           AND deleted_at IS NULL
        """ + tenantClause + """
         ORDER BY sold_at
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
   * Return one lookup-index row per ticket for the given period.
   * Used by {@code generateLookupRows()} in the archive provider.
   */
  public List<Map<String, Object>> findLookupRows(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    String sql = """
        SELECT id, tenant_id, public_code, sold_at
          FROM sales_ticket
         WHERE sold_at >= :from AND sold_at < :to
           AND deleted_at IS NULL
        """ + tenantClause + """
         ORDER BY sold_at
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to",   Timestamp.from(to));
    if (tenantId != null) params.addValue("tenantId", tenantId);

    List<Map<String, Object>> results = new ArrayList<>();
    jdbc.query(sql, params, rs -> {
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id",          rs.getObject("id",         UUID.class));
        row.put("tenant_id",   rs.getObject("tenant_id",  UUID.class));
        row.put("public_code", rs.getString("public_code"));
        Timestamp ts = rs.getTimestamp("sold_at");
        row.put("sold_at",     ts != null ? ts.toInstant() : null);
        results.add(row);
      }
      return null;
    });
    return results;
  }

  /** Count ticket lines whose parent ticket was sold in [from, to). */
  public long countLinesByTicketPeriod(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND t.tenant_id = :tenantId" : "";
    String sql = """
        SELECT COUNT(*)
          FROM sales_ticket_line tl
          JOIN sales_ticket t ON t.id = tl.ticket_id
         WHERE t.sold_at >= :from AND t.sold_at < :to
           AND t.deleted_at IS NULL
           AND tl.deleted_at IS NULL
        """ + tenantClause;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to",   Timestamp.from(to));
    if (tenantId != null) params.addValue("tenantId", tenantId);

    Long count = jdbc.queryForObject(sql, params, Long.class);
    return count != null ? count : 0L;
  }

  /** Stream ticket lines by parent ticket sold period. */
  public void streamLinesByTicketPeriod(Instant from, Instant to, UUID tenantId,
      Consumer<Map<String, Object>> consumer) {

    String tenantClause = tenantId != null ? " AND t.tenant_id = :tenantId" : "";
    String sql = """
        SELECT tl.id, tl.tenant_id, tl.ticket_id, t.public_code, t.sold_at,
               tl.draw_id, tl.line_number, tl.game_code, tl.bet_type, tl.bet_option,
               tl.selection_key, tl.display_selection, tl.stake_amount, tl.payout_base_amount,
               tl.odds_snapshot, tl.potential_payout_amount, tl.origin, tl.pricing_source,
               tl.selection_source, tl.promotion_decision_id, tl.promotion_label,
               tl.promotion_effect_type, tl.result_status, tl.payout_amount,
               tl.created_at, tl.created_by, tl.updated_at, tl.updated_by
          FROM sales_ticket_line tl
          JOIN sales_ticket t ON t.id = tl.ticket_id
         WHERE t.sold_at >= :from AND t.sold_at < :to
           AND t.deleted_at IS NULL
           AND tl.deleted_at IS NULL
        """ + tenantClause + """
         ORDER BY t.sold_at, tl.ticket_id, tl.line_number
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

  /** One lookup row per ticket that has archived line rows in the period. */
  public List<Map<String, Object>> findLineLookupRows(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND t.tenant_id = :tenantId" : "";
    String sql = """
        SELECT DISTINCT t.id, t.tenant_id, t.public_code, t.sold_at
          FROM sales_ticket t
          JOIN sales_ticket_line tl ON tl.ticket_id = t.id
         WHERE t.sold_at >= :from AND t.sold_at < :to
           AND t.deleted_at IS NULL
           AND tl.deleted_at IS NULL
        """ + tenantClause + """
         ORDER BY t.sold_at
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to",   Timestamp.from(to));
    if (tenantId != null) params.addValue("tenantId", tenantId);

    List<Map<String, Object>> results = new ArrayList<>();
    jdbc.query(sql, params, rs -> {
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id",          rs.getObject("id",         UUID.class));
        row.put("tenant_id",   rs.getObject("tenant_id",  UUID.class));
        row.put("public_code", rs.getString("public_code"));
        Timestamp ts = rs.getTimestamp("sold_at");
        row.put("sold_at",     ts != null ? ts.toInstant() : null);
        results.add(row);
      }
      return null;
    });
    return results;
  }
}
