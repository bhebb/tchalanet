package com.tchalanet.server.core.agent.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.agent.api.query.ListAgentsQuery;
import com.tchalanet.server.core.agent.api.query.model.AgentSummaryView;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentSummaryReadPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListAgentsQueryHandler implements QueryHandler<ListAgentsQuery, TchPage<AgentSummaryView>> {
  private final AgentSummaryReadPort readPort;

  @Override
  public TchPage<AgentSummaryView> handle(ListAgentsQuery query) {
    return readPort.findSummaries(query.pageable());
  }
}

