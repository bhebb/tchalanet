package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;

import java.time.Instant;

public record ListTicketsQuery(
    TerminalId terminalId,
    DrawId drawId,
    String status,
    Instant from,
    Instant to,
    TchPageRequest page
) implements Query<TchPage<TicketRow>> {
    public ListTicketsQuery {
        if (from != null && to != null && to.isBefore(from)) {
            throw ProblemRest.badRequest("ticket.filter.invalid_date_range");
        }
    }
}
