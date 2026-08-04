package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.query.CashierRecentTicketView;
import com.tchalanet.server.core.sales.api.query.CashierTopSelectionsView;
import com.tchalanet.server.core.sales.api.query.DrawTopSelectionsView;
import com.tchalanet.server.core.sales.internal.application.port.out.CashierTicketDashboardReaderPort;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashierTicketDashboardJdbcAdapter implements CashierTicketDashboardReaderPort {

  private final NamedParameterJdbcTemplate jdbc;

  private static final String RECENT_SQL =
      """
        SELECT t.public_code,
               t.sale_status,
               t.sold_at,
               t.stake_amount,
               dc.name  AS draw_channel_name,
               COUNT(tl.id) AS line_count
        FROM sales_ticket t
        LEFT JOIN draw_channel dc ON dc.id = t.draw_channel_id
        LEFT JOIN sales_ticket_line tl ON tl.ticket_id = t.id AND tl.deleted_at IS NULL
        WHERE t.seller_user_id = :cashierId
          AND t.deleted_at IS NULL
        GROUP BY t.id, t.public_code, t.sale_status, t.sold_at,
                 t.stake_amount, dc.name
        ORDER BY t.sold_at DESC
        LIMIT :limit
        """;

  private static final String TOP_SELECTIONS_SQL =
      """
        WITH ranked AS (
            SELECT tl.display_selection,
                   tl.game_code,
                   dc.code AS channel_code,
                   dc.name AS channel_label,
                   COUNT(*)                         AS sel_count,
                   COALESCE(SUM(tl.stake_amount), 0) AS total_stake,
                   ROW_NUMBER() OVER (
                       PARTITION BY t.draw_channel_id
                       ORDER BY COUNT(*) DESC
                   ) AS rn
            FROM sales_ticket_line tl
            JOIN sales_ticket t ON t.id = tl.ticket_id AND t.deleted_at IS NULL
            LEFT JOIN draw_channel dc ON dc.id = t.draw_channel_id
            WHERE t.seller_user_id = :cashierId
              AND t.sold_at::date = :businessDate
              AND tl.deleted_at IS NULL
            GROUP BY tl.display_selection, tl.game_code,
                     t.draw_channel_id, dc.code, dc.name
        )
        SELECT channel_code, channel_label, display_selection, game_code,
               sel_count, total_stake, rn
        FROM ranked
        WHERE rn <= :limitPerDraw
        ORDER BY channel_code, rn
        """;

  private static final String DRAW_TOP_SELECTIONS_SQL =
      """
        WITH ranked AS (
            SELECT tl.display_selection,
                   tl.game_code,
                   tl.bet_type,
                   tl.bet_option,
                   COUNT(*) AS sel_count,
                   COALESCE(SUM(tl.stake_amount), 0) AS total_stake,
                   ROW_NUMBER() OVER (
                       ORDER BY COUNT(*) DESC, COALESCE(SUM(tl.stake_amount), 0) DESC
                   ) AS rn
            FROM sales_ticket_line tl
            JOIN sales_ticket t ON t.id = tl.ticket_id AND t.deleted_at IS NULL
            WHERE tl.tenant_id = :tenantId
              AND tl.draw_id = :drawId
              AND tl.deleted_at IS NULL
              AND t.sale_status = 'APPROVED'
            GROUP BY tl.display_selection, tl.game_code, tl.bet_type, tl.bet_option
        )
        SELECT display_selection, game_code, bet_type, bet_option, sel_count, total_stake, rn
        FROM ranked
        WHERE rn <= :limit
        ORDER BY rn
        """;

  @Override
  public List<CashierRecentTicketView> findRecentByCashier(UserId cashierId, int limit) {
    var params =
        new MapSqlParameterSource()
            .addValue("cashierId", cashierId.value())
            .addValue("limit", limit);
    return jdbc.query(RECENT_SQL, params, (rs, i) -> mapRecentRow(rs));
  }

  @Override
  public CashierTopSelectionsView findTopSelections(
      UserId cashierId, LocalDate businessDate, int limitPerDraw) {
    var params =
        new MapSqlParameterSource()
            .addValue("cashierId", cashierId.value())
            .addValue("businessDate", businessDate)
            .addValue("limitPerDraw", limitPerDraw);

    // Ordered by channel_code then rn — group in Java
    Map<String, List<CashierTopSelectionsView.SelectionItem>> grouped = new LinkedHashMap<>();
    Map<String, String> channelLabels = new LinkedHashMap<>();

    jdbc.query(
        TOP_SELECTIONS_SQL,
        params,
        rs -> {
          String channelCode = rs.getString("channel_code");
          String channelLabel = rs.getString("channel_label");
          channelLabels.putIfAbsent(channelCode, channelLabel);
          grouped
              .computeIfAbsent(channelCode, k -> new ArrayList<>())
              .add(
                  new CashierTopSelectionsView.SelectionItem(
                      rs.getInt("rn"),
                      rs.getString("display_selection"),
                      rs.getString("game_code"),
                      rs.getInt("sel_count"),
                      toCents(rs.getBigDecimal("total_stake"))));
        });

    List<CashierTopSelectionsView.DrawGroup> byDraw =
        grouped.entrySet().stream()
            .map(
                e ->
                    new CashierTopSelectionsView.DrawGroup(
                        e.getKey(), channelLabels.get(e.getKey()), e.getValue()))
            .toList();

    return new CashierTopSelectionsView(businessDate, byDraw);
  }

  @Override
  public DrawTopSelectionsView findTopSelectionsByDraw(
      TenantId tenantId, DrawId drawId, int limit) {
    var params =
        new MapSqlParameterSource()
            .addValue("tenantId", tenantId.value())
            .addValue("drawId", drawId.value())
            .addValue("limit", limit);

    List<DrawTopSelectionsView.SelectionItem> items =
        jdbc.query(
            DRAW_TOP_SELECTIONS_SQL,
            params,
            (rs, i) -> {
              Number betOption = (Number) rs.getObject("bet_option");
              return new DrawTopSelectionsView.SelectionItem(
                  rs.getInt("rn"),
                  rs.getString("display_selection"),
                  rs.getString("game_code"),
                  rs.getString("bet_type"),
                  betOption == null ? null : betOption.shortValue(),
                  rs.getInt("sel_count"),
                  toCents(rs.getBigDecimal("total_stake")));
            });

    return new DrawTopSelectionsView(drawId, items);
  }

  private CashierRecentTicketView mapRecentRow(ResultSet rs) throws SQLException {
    var soldAtTs = rs.getTimestamp("sold_at");
    return new CashierRecentTicketView(
        rs.getString("public_code"),
        rs.getString("sale_status"),
        soldAtTs != null ? soldAtTs.toInstant() : null,
        toCents(rs.getBigDecimal("stake_amount")),
        rs.getString("draw_channel_name"),
        rs.getInt("line_count"));
  }

  private static long toCents(BigDecimal amount) {
    if (amount == null) return 0L;
    return amount.movePointRight(2).longValue();
  }
}
