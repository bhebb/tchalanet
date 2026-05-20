package com.tchalanet.server.core.session.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.api.query.CashierIdentityView;
import com.tchalanet.server.core.session.api.query.CashierSessionSummaryView;
import com.tchalanet.server.core.session.internal.application.port.out.CashierSessionDashboardReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CashierSessionDashboardJdbcAdapter implements CashierSessionDashboardReaderPort {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String SESSION_SUMMARY_SQL = """
        SELECT s.id::text           AS session_ref,
               s.opened_at,
               COALESCE(s.opening_float_cents, 0) AS opening_float_cents,
               COUNT(t.id)          AS ticket_count,
               COALESCE(SUM(t.total_amount), 0) AS sales_total
        FROM sales_session s
        LEFT JOIN sales_ticket t
               ON t.sales_session_id = s.id
              AND t.deleted_at IS NULL
              AND t.sale_status NOT IN ('CANCELLED', 'REJECTED')
        WHERE s.opened_by = :cashierId
          AND s.status = 'OPEN'
          AND s.deleted_at IS NULL
        GROUP BY s.id, s.opened_at, s.opening_float_cents
        ORDER BY s.opened_at DESC
        LIMIT 1
        """;

    @Override
    public Optional<CashierSessionSummaryView> findActiveSessionSummary(TenantId tenantId, UserId cashierId) {
        var params = new MapSqlParameterSource()
            .addValue("cashierId", cashierId.value());
        List<CashierSessionSummaryView> rows = jdbc.query(SESSION_SUMMARY_SQL, params, (rs, i) -> mapRow(rs));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private CashierSessionSummaryView mapRow(ResultSet rs) throws SQLException {
        String sessionRef = rs.getString("session_ref");
        var openedAtTs = rs.getTimestamp("opened_at");
        Instant openedAt = openedAtTs != null ? openedAtTs.toInstant() : null;
        long openingFloat = rs.getLong("opening_float_cents");
        int ticketCount = rs.getInt("ticket_count");
        long salesTotal = toCents(rs.getBigDecimal("sales_total"));
        return new CashierSessionSummaryView(true, sessionRef, openedAt, openingFloat, salesTotal, ticketCount);
    }

    private static final String IDENTITY_SQL = """
        SELECT u.display_name AS cashier_display_name,
               o.name         AS outlet_name,
               trm.label      AS terminal_label
        FROM sales_session s
        LEFT JOIN outlet   o   ON o.id   = s.outlet_id
        LEFT JOIN terminal trm ON trm.id = s.terminal_id
        LEFT JOIN app_user u   ON u.id   = s.opened_by
        WHERE s.opened_by  = :cashierId
          AND s.status     = 'OPEN'
          AND s.deleted_at IS NULL
        ORDER BY s.opened_at DESC
        LIMIT 1
        """;

    private static final String IDENTITY_FALLBACK_SQL = """
        SELECT display_name AS cashier_display_name
        FROM app_user
        WHERE id = :cashierId
        LIMIT 1
        """;

    @Override
    public CashierIdentityView findIdentity(TenantId tenantId, UserId cashierId) {
        var params = new MapSqlParameterSource().addValue("cashierId", cashierId.value());
        List<CashierIdentityView> rows = jdbc.query(IDENTITY_SQL, params, (rs, i) ->
            new CashierIdentityView(
                rs.getString("cashier_display_name"),
                rs.getString("outlet_name"),
                rs.getString("terminal_label"),
                tenantId.value().toString()
            ));
        if (!rows.isEmpty()) return rows.get(0);

        // Fallback: no open session — return user name only
        List<String> names = jdbc.query(IDENTITY_FALLBACK_SQL, params,
            (rs, i) -> rs.getString("cashier_display_name"));
        String displayName = names.isEmpty() ? null : names.get(0);
        return new CashierIdentityView(displayName, null, null, tenantId.value().toString());
    }

    private static long toCents(java.math.BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.movePointRight(2).longValue();
    }
}
