package com.tchalanet.server.core.session.internal.infra.persistence.adapter;

import com.tchalanet.server.common.config.TchSystemProperties;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.application.port.out.AutoSessionTargetReaderPort;
import com.tchalanet.server.core.session.internal.domain.model.AutoSessionCloseTarget;
import com.tchalanet.server.core.session.internal.domain.model.AutoSessionOpenTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Scheduler read-model projection.
// Intentionally uses SQL joins across outlet/terminal for read-only auto-session targeting.
// No cross-domain writes and no dependency on other domains' infra repositories/entities.
@Component
@RequiredArgsConstructor
public class AutoSessionTargetJdbcAdapter implements AutoSessionTargetReaderPort {

    private final NamedParameterJdbcTemplate jdbc;
    private final TchSystemProperties systemProperties;

    @Override
    public List<AutoSessionOpenTarget> findDueOpenTargets(Instant now) {
        var sql = """
            select
              o.tenant_id,
              o.id as outlet_id,
              t.id as terminal_id,
              t.assigned_user_id,
              o.timezone,
              o.session_open_time,
              o.default_opening_float_cents
            from outlet o
            join terminal t
              on t.tenant_id = o.tenant_id
             and t.outlet_id = o.id
            where o.deleted_at is null
              and t.deleted_at is null
              and o.auto_session_open_enabled = true
              and o.session_open_time is not null
              and t.assigned_user_id is not null
              and t.auto_session_enabled = true
              and t.state = 'ACTIVE'
            """;

        return jdbc.query(sql, Map.of(), (rs, i) -> {
                var timezone = rs.getString("timezone");
                var openTime = rs.getObject("session_open_time", LocalTime.class);

                if (!isDue(timezone, openTime, now)) {
                    return null;
                }

                var businessDate = now.atZone(ZoneId.of(timezone)).toLocalDate();

                return new AutoSessionOpenTarget(
                    TenantId.of(rs.getObject("tenant_id", UUID.class)),
                    OutletId.of(rs.getObject("outlet_id", UUID.class)),
                    TerminalId.of(rs.getObject("terminal_id", UUID.class)),
                    UserId.of(rs.getObject("assigned_user_id", UUID.class)),
                    businessDate,
                    now,
                    getNullableLong(rs, "default_opening_float_cents"));
            }).stream()
            .filter(Objects::nonNull)
            .toList();
    }


    @Override
    public List<AutoSessionCloseTarget> findDueCloseTargets(Instant now) {
        var sql = """
            select
              s.tenant_id,
              s.id as session_id,
              s.outlet_id,
              s.terminal_id,
              s.opened_by as closed_by,
              o.timezone,
              o.session_close_time
            from sales_session s
            join outlet o
              on o.tenant_id = s.tenant_id
             and o.id = s.outlet_id
            where s.deleted_at is null
              and o.deleted_at is null
              and s.status = 'OPEN'
              and o.auto_session_close_enabled = true
              and o.session_close_time is not null
            """;

        return jdbc.query(sql, Map.of(), (rs, i) -> {
                var timezone = rs.getString("timezone");
                var closeTime = rs.getObject("session_close_time", LocalTime.class);

                if (!isDue(timezone, closeTime, now)) {
                    return null;
                }

                return new AutoSessionCloseTarget(
                    TenantId.of(rs.getObject("tenant_id", UUID.class)),
                    SalesSessionId.of(rs.getObject("session_id", UUID.class)),
                    OutletId.of(rs.getObject("outlet_id", UUID.class)),
                    TerminalId.of(rs.getObject("terminal_id", UUID.class)),
                    UserId.of(rs.getObject("closed_by", UUID.class)),
                    now,
                    "Auto session close");
            }).stream()
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public List<AutoSessionCloseTarget> findOpenCloseTargetsByOutlet(
        TenantId tenantId,
        OutletId outletId,
        Instant closedAt,
        UserId closedBy,
        String reason) {

        var sql = """
      select
        s.tenant_id,
        s.id as session_id,
        s.outlet_id,
        s.terminal_id,
        coalesce(:closed_by, s.opened_by) as closed_by
      from sales_session s
      where s.deleted_at is null
        and s.tenant_id = :tenant_id
        and s.outlet_id = :outlet_id
        and s.status = 'OPEN'
      """;

        var params =
            Map.of(
                "tenant_id", tenantId.value(),
                "outlet_id", outletId.value(),
                "closed_by", closedBy == null ? systemProperties.systemUserId() : closedBy.value());

        return jdbc.query(
            sql,
            params,
            (rs, i) ->
                new AutoSessionCloseTarget(
                    TenantId.of(rs.getObject("tenant_id", UUID.class)),
                    SalesSessionId.of(rs.getObject("session_id", UUID.class)),
                    OutletId.of(rs.getObject("outlet_id", UUID.class)),
                    TerminalId.of(rs.getObject("terminal_id", UUID.class)),
                    UserId.of(rs.getObject("closed_by", UUID.class)),
                    closedAt,
                    reason));
    }


    private boolean isDue(String timezone, LocalTime dueTime, Instant now) {
        if (timezone == null || timezone.isBlank() || dueTime == null) {
            return false;
        }

        var localNow = now.atZone(ZoneId.of(timezone)).toLocalTime();
        var windowStart = dueTime;
        var windowEnd = dueTime.plusMinutes(5);

        return !localNow.isBefore(windowStart) && localNow.isBefore(windowEnd);
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        var value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
