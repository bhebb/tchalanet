package com.tchalanet.server.core.session.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.application.port.out.SalesSessionOpeningContextReaderPort;
import com.tchalanet.server.core.session.internal.domain.model.SalesSessionOpeningContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Loads all session-opening eligibility facts in a single SQL query.
 *
 * <p>Uses {@code SELECT 1} as seed with multiple LEFT JOINs so that missing
 * entities produce {@code false} flags rather than no row. The result is
 * evaluated by {@code SalesSessionOpeningEligibilityPolicy}.
 *
 * <p>Schema notes:
 * <ul>
 *   <li>{@code outlet.outlet_blocked} — dedicated blocked flag (distinct from sales_blocked)
 *   <li>{@code outlet.day_closed} — set by {@code CloseOutletDayCommandHandler}; drives businessDayOpen
 *   <li>{@code terminal.state} — not {@code status}
 *   <li>{@code terminal.binding_status} → join {@code terminal_binding.status = 'ACTIVE'}
 *   <li>{@code seller_outlet_assignment} joins via {@code seller.user_id}, not directly on user
 *   <li>{@code seller_terminal_assignment} → {@code terminal_assignment.user_id}
 *   <li>{@code tenant_user.pos_enabled} does not exist → {@code seller.status = 'ACTIVE'} (V1)
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JdbcSalesSessionOpeningContextAdapter
    implements SalesSessionOpeningContextReaderPort {

    private static final String SQL = """
        SELECT
            /* tenant */
            (t.id IS NOT NULL)                               AS tenant_exists,
            COALESCE(t.status = 'ACTIVE', false)             AS tenant_active,

            /* app user */
            (u.id IS NOT NULL)                               AS user_exists,
            COALESCE(u.status = 'ACTIVE', false)             AS user_active,

            /* seller — POS identity of the user within the tenant */
            (s.id IS NOT NULL)                               AS seller_exists_in_tenant,
            COALESCE(s.status = 'ACTIVE', false)             AS seller_active_in_tenant,
            COALESCE(s.status = 'ACTIVE', false)             AS seller_can_open_pos_session,

            /* outlet */
            (o.id IS NOT NULL)                               AS outlet_exists,
            COALESCE(o.tenant_id = :tenant_id, false)        AS outlet_belongs_to_tenant,
            COALESCE(o.status = 'ACTIVE', false)             AS outlet_active,
            COALESCE(o.outlet_blocked = true, false)         AS outlet_blocked,

            /* terminal */
            (term.id IS NOT NULL)                            AS terminal_exists,
            COALESCE(term.tenant_id = :tenant_id, false)     AS terminal_belongs_to_tenant,
            COALESCE(term.outlet_id = :outlet_id, false)     AS terminal_belongs_to_outlet,
            COALESCE(term.state = 'ACTIVE', false)           AS terminal_active,
            COALESCE(term.sales_blocked = true, false)       AS terminal_blocked,
            (tb.id IS NOT NULL)                              AS terminal_bound,

            /* seller assignments */
            (soa.id IS NOT NULL)                             AS seller_allowed_for_outlet,
            (ta.id IS NOT NULL)                              AS seller_allowed_for_terminal,

            /* business calendar — outlet.day_closed is set by CloseOutletDayCommandHandler */
            COALESCE(NOT o.day_closed, true)                 AS business_day_open,

            /* current open session */
            open_session.id                                  AS current_open_session_id

        FROM (SELECT 1) seed

        LEFT JOIN tenant t
               ON t.id           = :tenant_id
              AND t.deleted_at   IS NULL

        LEFT JOIN app_user u
               ON u.id           = :seller_user_id
              AND u.deleted_at   IS NULL

        LEFT JOIN seller s
               ON s.user_id      = :seller_user_id
              AND s.tenant_id    = :tenant_id
              AND s.deleted_at   IS NULL

        LEFT JOIN outlet o
               ON o.id           = :outlet_id
              AND o.deleted_at   IS NULL

        LEFT JOIN terminal term
               ON term.id        = :terminal_id
              AND term.deleted_at IS NULL

        LEFT JOIN terminal_binding tb
               ON tb.terminal_id = :terminal_id
              AND tb.status      = 'ACTIVE'
              AND tb.deleted_at  IS NULL

        LEFT JOIN seller_outlet_assignment soa
               ON soa.seller_id  = s.id
              AND soa.outlet_id  = :outlet_id
              AND soa.status     = 'ACTIVE'
              AND soa.deleted_at IS NULL

        LEFT JOIN terminal_assignment ta
               ON ta.terminal_id = :terminal_id
              AND ta.user_id     = :seller_user_id
              AND ta.status      = 'ACTIVE'
              AND ta.deleted_at  IS NULL

        LEFT JOIN sales_session open_session
               ON open_session.tenant_id   = :tenant_id
              AND open_session.outlet_id   = :outlet_id
              AND open_session.terminal_id = :terminal_id
              AND open_session.opened_by   = :seller_user_id
              AND open_session.status      = 'OPEN'
              AND open_session.deleted_at  IS NULL

        LIMIT 1
        """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public SalesSessionOpeningContext loadForOpening(
        TenantId tenantId,
        OutletId outletId,
        TerminalId terminalId,
        UserId openedBy,
        LocalDate businessDate
    ) {
        var params = new MapSqlParameterSource()
            .addValue("tenant_id",     tenantId.value())
            .addValue("outlet_id",     outletId.value())
            .addValue("terminal_id",   terminalId.value())
            .addValue("seller_user_id", openedBy.value());

        return jdbc.queryForObject(SQL, params, (rs, rowNum) ->
            new SalesSessionOpeningContext(
                tenantId,
                outletId,
                terminalId,
                openedBy,

                rs.getBoolean("tenant_exists"),
                rs.getBoolean("tenant_active"),

                rs.getBoolean("user_exists"),
                rs.getBoolean("user_active"),

                rs.getBoolean("seller_exists_in_tenant"),
                rs.getBoolean("seller_active_in_tenant"),
                rs.getBoolean("seller_can_open_pos_session"),

                rs.getBoolean("outlet_exists"),
                rs.getBoolean("outlet_belongs_to_tenant"),
                rs.getBoolean("outlet_active"),
                rs.getBoolean("outlet_blocked"),

                rs.getBoolean("terminal_exists"),
                rs.getBoolean("terminal_belongs_to_tenant"),
                rs.getBoolean("terminal_belongs_to_outlet"),
                rs.getBoolean("terminal_active"),
                rs.getBoolean("terminal_blocked"),
                rs.getBoolean("terminal_bound"),

                rs.getBoolean("seller_allowed_for_outlet"),
                rs.getBoolean("seller_allowed_for_terminal"),

                rs.getBoolean("business_day_open"),

                Optional.ofNullable((UUID) rs.getObject("current_open_session_id"))
                    .map(SalesSessionId::of)
            )
        );
    }
}
