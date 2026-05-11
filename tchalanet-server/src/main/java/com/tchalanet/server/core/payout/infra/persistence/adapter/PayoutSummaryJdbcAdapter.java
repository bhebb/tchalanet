package com.tchalanet.server.core.payout.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.payout.application.port.out.PayoutSummaryReaderPort;
import com.tchalanet.server.core.payout.application.query.model.PayoutSessionSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
class PayoutSummaryJdbcAdapter implements PayoutSummaryReaderPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public PayoutSessionSummary getByPayingSession(TenantId tenantId, SalesSessionId sessionId) {
        var sql = """
        select
          count(*) filter (where status = 'REQUESTED') as requested_count,
          count(*) filter (where status = 'APPROVED') as approved_count,
          count(*) filter (where status = 'REJECTED') as rejected_count,
          count(*) filter (where status = 'PAID') as paid_count,
          coalesce(sum(amount_cents) filter (where status = 'PAID'), 0) as paid_amount_cents,
          count(*) filter (where status = 'CANCELLED') as cancelled_count
        from payout
        where tenant_id = :tenantId
          and paying_session_id = :sessionId
        """;

        return jdbc.queryForObject(
            sql,
            Map.of("tenantId", tenantId.value(), "sessionId", sessionId.value()),
            (rs, rowNum) ->
                new PayoutSessionSummary(
                    rs.getLong("requested_count"),
                    rs.getLong("approved_count"),
                    rs.getLong("rejected_count"),
                    rs.getLong("paid_count"),
                    rs.getLong("paid_amount_cents"),
                    rs.getLong("cancelled_count")));
    }
}
