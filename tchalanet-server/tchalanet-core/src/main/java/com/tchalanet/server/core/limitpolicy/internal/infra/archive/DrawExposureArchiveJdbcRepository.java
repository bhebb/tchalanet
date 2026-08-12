package com.tchalanet.server.core.limitpolicy.internal.infra.archive;

import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for archiving {@code draw_exposure} rows whose associated draw is ARCHIVED.
 *
 * <p>Archive by draw lifecycle, not by simple time period. Once a draw is ARCHIVED its exposure
 * projection rows are never read again by any runtime limitpolicy query.
 */
@Repository
@RequiredArgsConstructor
public class DrawExposureArchiveJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  /** Count draw_exposure rows belonging to draws archived before the period end. */
  public long countByArchivedDraw(Instant periodEnd, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND de.tenant_id = :tenantId" : "";
    String sql =
        """
        SELECT COUNT(*)
          FROM draw_exposure de
          JOIN draw d ON d.id = de.draw_id
         WHERE d.status = 'ARCHIVED'
           AND d.scheduled_at < :periodEnd
        """
            + tenantClause;

    Long count = jdbc.queryForObject(sql, params(periodEnd, tenantId), Long.class);
    return count != null ? count : 0L;
  }

  /**
   * Stream draw_exposure rows for draws archived before the period end, then hard-delete them.
   *
   * <p>Hard-delete is safe: these rows are stateful projection data. Once a draw is ARCHIVED, no
   * runtime query will ever re-read them (all runtime queries filter on draw_id of an active draw).
   */
  public long streamAndDelete(
      Instant periodEnd, UUID tenantId, Consumer<Map<String, Object>> rowSink) {

    String tenantClause = tenantId != null ? " AND de.tenant_id = :tenantId" : "";
    String selectSql =
        """
        SELECT de.id, de.tenant_id, de.draw_id, de.scope_type, de.scope_id,
               de.bet_type, de.selection_key, de.stake_total, de.sales_count,
               de.last_event_id, de.last_event_at
          FROM draw_exposure de
          JOIN draw d ON d.id = de.draw_id
         WHERE d.status = 'ARCHIVED'
           AND d.scheduled_at < :periodEnd
        """
            + tenantClause
            + """
         ORDER BY de.draw_id, de.scope_type, de.scope_id, de.selection_key
        """;

    long[] exported = {0};
    jdbc.query(
        selectSql,
        params(periodEnd, tenantId),
        rs -> {
          ResultSetMetaData meta = rs.getMetaData();
          int cols = meta.getColumnCount();
          while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>(cols * 2);
            for (int i = 1; i <= cols; i++) {
              Object val = rs.getObject(i);
              if (val instanceof Timestamp ts) val = ts.toInstant();
              row.put(meta.getColumnLabel(i), val);
            }
            rowSink.accept(row);
            exported[0]++;
          }
          return null;
        });

    if (exported[0] > 0) {
      String deleteSql =
          """
          DELETE FROM draw_exposure de
          USING draw d
          WHERE d.id = de.draw_id
            AND d.status = 'ARCHIVED'
            AND d.scheduled_at < :periodEnd
          """
              + (tenantId != null ? " AND de.tenant_id = :tenantId" : "");

      jdbc.update(deleteSql, params(periodEnd, tenantId));
    }

    return exported[0];
  }

  private MapSqlParameterSource params(Instant periodEnd, UUID tenantId) {
    var p = new MapSqlParameterSource().addValue("periodEnd", Timestamp.from(periodEnd));
    if (tenantId != null) {
      p.addValue("tenantId", tenantId);
    }
    return p;
  }
}
