package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sales.api.error.SalesErrorCodes;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import java.time.Instant;

public record ListTicketsQuery(
    SellerTerminalId sellerTerminalId,
    DrawId drawId,
    String status,
    String resultStatus,
    String settlementStatus,
    Boolean winningOnly,
    String q,
    Instant from,
    Instant to,
    TchPageRequest page)
    implements Query<TchPage<TicketRow>> {
  public ListTicketsQuery {
    if (from != null && to != null && to.isBefore(from)) {
      throw ProblemRest.of(SalesErrorCodes.TICKET_FILTER_INVALID_DATE_RANGE);
    }
  }
}
