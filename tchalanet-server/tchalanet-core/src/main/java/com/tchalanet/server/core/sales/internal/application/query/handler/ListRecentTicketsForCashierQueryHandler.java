package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.ListRecentTicketsForCashierQuery;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListRecentTicketsForCashierQueryHandler
    implements QueryHandler<ListRecentTicketsForCashierQuery, List<Ticket>> {

  private final TicketReaderPort ticketReader;

  @Override
  public List<Ticket> handle(ListRecentTicketsForCashierQuery query) {
    return ticketReader.listRecentForCashier(query.cashierId(), query.limit());
  }
}
