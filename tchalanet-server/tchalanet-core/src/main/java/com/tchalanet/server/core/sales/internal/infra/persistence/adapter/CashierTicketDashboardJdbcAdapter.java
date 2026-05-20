package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.query.CashierDashboardOverviewView;
import com.tchalanet.server.core.sales.api.query.CashierPendingApprovalView;
import com.tchalanet.server.core.sales.api.query.CashierRecentTicketView;
import com.tchalanet.server.core.sales.api.query.CashierTopSelectionsView;
import com.tchalanet.server.core.sales.internal.application.port.out.CashierTicketDashboardReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CashierTicketDashboardJdbcAdapter implements CashierTicketDashboardReaderPort {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String RECENT_SQL = """
        SELECT t.public_code,
               t.sale_status,
               t.sold_at,
               t.stake_amount,
               t.potential_payout_amount,
               dc.name  AS draw_channel_name,
               COUNT(tl.id) AS line_count
        FROM sales_ticket t
        LEFT JOIN draw_channel dc ON dc.id = t.draw_channel_id
        LEFT JOIN sales_ticket_line tl ON tl.ticket_id = t.id AND tl.deleted_at IS NULL
        WHERE t.seller_user_id = :cashierId
          AND t.deleted_at IS NULL
        GROUP BY t.id, t.public_code, t.sale_status, t.sold_at,
                 t.stake_amount, t.potential_payout_amount, dc.name
        ORDER BY t.sold_at DESC
        LIMIT :limit
        """;

    private static final String OVERVIEW_SQL = """
        SELECT COUNT(*)                                                AS ticket_count,
               COALESCE(SUM(t.total_amount), 0)                      AS sales_total,
               COUNT(*) FILTER (WHERE t.sale_status = 'CANCELLED')   AS cancelled_count,
               COUNT(*) FILTER (WHERE t.sale_status = 'PENDING_APPROVAL') AS pending_count
        FROM sales_ticket t
        WHERE t.seller_user_id = :cashierId
          AND t.deleted_at IS NULL
          AND t.sold_at::date = :businessDate
        """;

    private static final String BY_DRAW_SQL = """
        SELECT dc.code AS channel_code,
               dc.name AS channel_label,
               COUNT(*) AS ticket_count,
               COALESCE(SUM(t.total_amount), 0) AS sales_total
        FROM sales_ticket t
        LEFT JOIN draw_channel dc ON dc.id = t.draw_channel_id
        WHERE t.seller_user_id = :cashierId
          AND t.deleted_at IS NULL
          AND t.sold_at::date = :businessDate
        GROUP BY dc.id, dc.code, dc.name
        ORDER BY SUM(t.total_amount) DESC
        """;

    private static final String TOP_SELECTIONS_SQL = """
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

    private static final String PENDING_APPROVALS_SQL = """
        SELECT t.public_code,
               t.total_amount,
               dc.name AS draw_channel_name,
               t.approval_requested_at
        FROM sales_ticket t
        LEFT JOIN draw_channel dc ON dc.id = t.draw_channel_id
        WHERE t.seller_user_id = :cashierId
          AND t.sale_status = 'PENDING_APPROVAL'
          AND t.deleted_at IS NULL
        ORDER BY t.approval_requested_at DESC
        LIMIT :limit
        """;

    @Override
    public List<CashierRecentTicketView> findRecentByCashier(UserId cashierId, int limit) {
        var params = new MapSqlParameterSource()
            .addValue("cashierId", cashierId.value())
            .addValue("limit", limit);
        return jdbc.query(RECENT_SQL, params, (rs, i) -> mapRecentRow(rs));
    }

    @Override
    public CashierDashboardOverviewView getOverview(TenantId tenantId, UserId cashierId, LocalDate businessDate) {
        var params = new MapSqlParameterSource()
            .addValue("cashierId", cashierId.value())
            .addValue("businessDate", businessDate);

        var totals = jdbc.queryForObject(OVERVIEW_SQL, params, (rs, i) -> {
            long ticketCount = rs.getLong("ticket_count");
            BigDecimal salesTotal = rs.getBigDecimal("sales_total");
            long cancelledCount = rs.getLong("cancelled_count");
            long pendingCount = rs.getLong("pending_count");
            return new long[]{ticketCount, toCents(salesTotal), cancelledCount, pendingCount};
        });

        List<CashierDashboardOverviewView.DrawBreakdown> byDraw =
            jdbc.query(BY_DRAW_SQL, params, (rs, i) ->
                new CashierDashboardOverviewView.DrawBreakdown(
                    rs.getString("channel_code"),
                    rs.getString("channel_label"),
                    rs.getLong("ticket_count"),
                    toCents(rs.getBigDecimal("sales_total"))
                ));

        return new CashierDashboardOverviewView(
            businessDate,
            totals[0], totals[1], totals[2], totals[3],
            byDraw
        );
    }

    @Override
    public CashierTopSelectionsView findTopSelections(UserId cashierId, LocalDate businessDate, int limitPerDraw) {
        var params = new MapSqlParameterSource()
            .addValue("cashierId", cashierId.value())
            .addValue("businessDate", businessDate)
            .addValue("limitPerDraw", limitPerDraw);

        // Ordered by channel_code then rn — group in Java
        Map<String, List<CashierTopSelectionsView.SelectionItem>> grouped = new LinkedHashMap<>();
        Map<String, String> channelLabels = new LinkedHashMap<>();

        jdbc.query(TOP_SELECTIONS_SQL, params, rs -> {
            String channelCode = rs.getString("channel_code");
            String channelLabel = rs.getString("channel_label");
            channelLabels.putIfAbsent(channelCode, channelLabel);
            grouped.computeIfAbsent(channelCode, k -> new ArrayList<>())
                .add(new CashierTopSelectionsView.SelectionItem(
                    rs.getInt("rn"),
                    rs.getString("display_selection"),
                    rs.getString("game_code"),
                    rs.getInt("sel_count"),
                    toCents(rs.getBigDecimal("total_stake"))
                ));
        });

        List<CashierTopSelectionsView.DrawGroup> byDraw = grouped.entrySet().stream()
            .map(e -> new CashierTopSelectionsView.DrawGroup(
                e.getKey(),
                channelLabels.get(e.getKey()),
                e.getValue()))
            .toList();

        return new CashierTopSelectionsView(businessDate, byDraw);
    }

    @Override
    public List<CashierPendingApprovalView> findPendingApprovals(UserId cashierId, int limit) {
        var params = new MapSqlParameterSource()
            .addValue("cashierId", cashierId.value())
            .addValue("limit", limit);
        return jdbc.query(PENDING_APPROVALS_SQL, params, (rs, i) -> {
            var submittedAtTs = rs.getTimestamp("approval_requested_at");
            return new CashierPendingApprovalView(
                rs.getString("public_code"),
                toCents(rs.getBigDecimal("total_amount")),
                rs.getString("draw_channel_name"),
                submittedAtTs != null ? submittedAtTs.toInstant() : null
            );
        });
    }

    private CashierRecentTicketView mapRecentRow(ResultSet rs) throws SQLException {
        var soldAtTs = rs.getTimestamp("sold_at");
        return new CashierRecentTicketView(
            rs.getString("public_code"),
            rs.getString("sale_status"),
            soldAtTs != null ? soldAtTs.toInstant() : null,
            toCents(rs.getBigDecimal("stake_amount")),
            toCents(rs.getBigDecimal("potential_payout_amount")),
            rs.getString("draw_channel_name"),
            rs.getInt("line_count")
        );
    }

    private static long toCents(BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.movePointRight(2).longValue();
    }
}
