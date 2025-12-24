package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto;
import com.tchalanet.server.core.sales.application.query.model.GetAgentDailySalesQuery;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler that returns an agent's daily sales summary. */
@UseCase
@RequiredArgsConstructor
@Component
public class GetAgentDailySalesQueryHandler implements QueryHandler<GetAgentDailySalesQuery, Optional<AgentDailySalesDto>> {

  private final TicketReaderPort ticketReader;
  private final Clock clock;

  @Override
  public Optional<AgentDailySalesDto> handle(GetAgentDailySalesQuery query) {
    Instant from = query.date().atStartOfDay(clock.getZone()).toInstant();
    Instant to = query.date().plusDays(1).atStartOfDay(clock.getZone()).toInstant();

    return ticketReader.getAgentDailySales(query.tenantId(), from, to)
        .stream()
        .filter(dto -> dto.agentId().equals(query.agentId()))
        .findFirst();
  }
}
