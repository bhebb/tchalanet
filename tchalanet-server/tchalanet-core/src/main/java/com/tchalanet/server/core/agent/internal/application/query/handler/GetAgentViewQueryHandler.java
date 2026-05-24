package com.tchalanet.server.core.agent.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.agent.api.model.AgentView;
import com.tchalanet.server.core.agent.api.query.GetAgentViewQuery;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentReaderPort;
import com.tchalanet.server.core.agent.internal.application.service.AgentApplicationMapper;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetAgentViewQueryHandler implements QueryHandler<GetAgentViewQuery, AgentView> {
  private final AgentReaderPort reader;
  private final AgentApplicationMapper mapper = new AgentApplicationMapper();
  @Override public AgentView handle(GetAgentViewQuery q) { return mapper.toAgentView(reader.getAgentRequired(q.tenantId(), q.agentId())); }
}
