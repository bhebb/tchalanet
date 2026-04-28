package com.tchalanet.server.core.session.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.query.model.ListCashierOpenSessionsQuery;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListCashierOpenSessionsHandler
    implements QueryHandler<
        ListCashierOpenSessionsQuery, List<ListCashierOpenSessionsHandler.CashierSessionDto>> {

  private final SalesSessionReaderPort sessionReader;

  public record CashierSessionDto(
      SessionId sessionId,
      String channelCode,
      java.time.Instant openedAt,
      java.math.BigDecimal totalSales,
      long ticketsSold) {}

  @Override
  public List<CashierSessionDto> handle(ListCashierOpenSessionsQuery query) {
    var sessions = sessionReader.findOpenByCashier(query.tenantId(), query.userId());
    return sessions.stream()
        .map(
            s ->
                new CashierSessionDto(
                    s.id(),
                    null, // drawChannelCode not available
                    s.openedAt(),
                    s.totalStake(), // assuming totalStake is totalSales
                    s.totalTickets() != null ? s.totalTickets() : 0L))
        .collect(Collectors.toList());
  }
}
