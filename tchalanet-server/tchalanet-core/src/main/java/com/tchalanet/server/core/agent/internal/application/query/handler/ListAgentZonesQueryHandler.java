package com.tchalanet.server.core.agent.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.agent.api.model.AgentZoneView;
import com.tchalanet.server.core.agent.api.query.ListAgentZonesQuery;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentReaderPort;
import com.tchalanet.server.core.agent.internal.application.service.AgentApplicationMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListAgentZonesQueryHandler implements QueryHandler<ListAgentZonesQuery, List<AgentZoneView>> {
  private final AgentReaderPort reader;
  private final AgentApplicationMapper mapper = new AgentApplicationMapper();
  @Override public List<AgentZoneView> handle(ListAgentZonesQuery q) { return reader.listZones(q.tenantId(), q.activeOnly()).stream().map(mapper::toZoneView).toList(); }
}
