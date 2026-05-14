package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.sales.api.model.TicketResultStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.paging.TchPage;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

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
