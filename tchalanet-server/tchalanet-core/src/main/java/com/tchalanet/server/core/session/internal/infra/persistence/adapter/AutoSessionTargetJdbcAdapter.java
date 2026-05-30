package com.tchalanet.server.core.session.internal.infra.persistence.adapter;

import com.tchalanet.server.common.context.system.SystemContextProperties;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.application.port.out.AutoSessionTargetReaderPort;
import com.tchalanet.server.core.session.internal.domain.model.AutoSessionCloseTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Scheduler read-model projection.
// No cross-domain writes and no dependency on other domains' infra repositories/entities.
// RLS context must be bound by the caller (CloseDueSalesSessionsCommandHandler) before invoking.
@Component
@RequiredArgsConstructor
public class AutoSessionTargetJdbcAdapter implements AutoSessionTargetReaderPort {

    private final NamedParameterJdbcTemplate jdbc;
    private final SystemContextProperties systemProperties;

    @Override
    public List<AutoSessionCloseTarget> findOpenSessionsBeforeBusinessDate(
        TenantId tenantId,
        LocalDate cutoffDate,
        Instant closedAt,
        String reason) {

        // Closes all OPEN sessions whose businessDate is strictly before cutoffDate.
        // The caller passes tenantToday as cutoffDate so only previous-day sessions are targeted —
        // the current business day is never touched.
        var sql = """
            select
              s.tenant_id,
              s.id          as session_id,
              s.outlet_id,
              s.terminal_id,
              s.opened_by   as closed_by
            from sales_session s
            where s.deleted_at   is null
              and s.tenant_id    = :tenant_id
              and s.status       = 'OPEN'
              and s.business_date < :cutoff_date
            """;

        var params = Map.of(
            "tenant_id",   tenantId.value(),
            "cutoff_date", cutoffDate);

        return jdbc.query(sql, params,
            (rs, i) -> new AutoSessionCloseTarget(
                TenantId.of(rs.getObject("tenant_id", UUID.class)),
                SalesSessionId.of(rs.getObject("session_id", UUID.class)),
                OutletId.of(rs.getObject("outlet_id", UUID.class)),
                TerminalId.of(rs.getObject("terminal_id", UUID.class)),
                UserId.of(rs.getObject("closed_by", UUID.class)),
                closedAt,
                reason));
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


}
