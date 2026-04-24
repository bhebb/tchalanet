package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.paging.TchPage;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Query to list tickets.
 */
public record ListTicketsQuery(TicketFilter filter, Pageable pageRequest)
    implements Query<TchPage<TicketSummaryView>> {

    /**
     * Filter for tickets.
     */
    public record TicketFilter(
        TenantId tenantId,
        TerminalId terminalId, // optional
        DrawId drawId, // optional
        TicketResultStatus status, // optional - composite status
        Instant from, // optional
        Instant to // optional
    ) {
    }


}
