package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.application.port.out.TicketSalesSummaryReaderPort;
import com.tchalanet.server.core.sales.application.query.model.TicketSalesSessionSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
class TicketSalesSummaryJdbcAdapter implements TicketSalesSummaryReaderPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public TicketSalesSessionSummary getBySession(TenantId tenantId, SalesSessionId sessionId) {
        var sql = """
        select
          count(*) filter (where sale_status = 'SOLD') as sold_count,
          coalesce(sum(total_amount) filter (where sale_status = 'SOLD'), 0) as sold_amount,
          count(*) filter (where sale_status = 'PENDING_APPROVAL') as pending_approval_count,
          count(*) filter (where sale_status = 'VOID') as void_count,
          count(*) filter (where sale_status = 'REJECTED') as rejected_count,
          count(*) filter (where result_status = 'WON') as won_count,
          coalesce(sum(winning_amount) filter (where result_status = 'WON'), 0) as won_amount,
          count(*) filter (where result_status = 'LOST') as lost_count,
          count(*) filter (where settlement_status = 'SETTLED') as settled_count
        from ticket
        where tenant_id = :tenantId
          and session_id = :sessionId
        """;

        return jdbc.queryForObject(
            sql,
            Map.of("tenantId", tenantId.value(), "sessionId", sessionId.value()),
            (rs, rowNum) ->
                new TicketSalesSessionSummary(
                    rs.getLong("sold_count"),
                    toCents(rs.getBigDecimal("sold_amount")),
                    rs.getLong("pending_approval_count"),
                    rs.getLong("void_count"),
                    rs.getLong("rejected_count"),
                    rs.getLong("won_count"),
                    toCents(rs.getBigDecimal("won_amount")),
                    rs.getLong("lost_count"),
                    rs.getLong("settled_count")));
    }

    private long toCents(BigDecimal amount) {
        return amount == null ? 0L : amount.movePointRight(2).longValueExact();
    }
}
