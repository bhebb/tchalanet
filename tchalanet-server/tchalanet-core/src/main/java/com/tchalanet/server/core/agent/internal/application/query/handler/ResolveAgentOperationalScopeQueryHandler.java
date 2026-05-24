package com.tchalanet.server.core.agent.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.agent.api.model.AgentOperationalScopeView;
import com.tchalanet.server.core.agent.api.query.ResolveAgentOperationalScopeQuery;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentReaderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ResolveAgentOperationalScopeQueryHandler implements QueryHandler<ResolveAgentOperationalScopeQuery, AgentOperationalScopeView> {
  private final AgentReaderPort reader;

  @Override
  public AgentOperationalScopeView handle(ResolveAgentOperationalScopeQuery q) {
    var agent = reader.findAgentForSeller(q.tenantId(), q.sellerUserId())
        .orElseThrow(() -> new IllegalArgumentException("agent.no_agent_for_seller"));
    var zone = reader.getZoneRequired(q.tenantId(), agent.primaryZoneId());
    var warnings = agent.activeForSale() ? List.<String>of() : List.of("agent.not_active");
    return new AgentOperationalScopeView(agent.id(), reader.agentPath(q.tenantId(), agent.id()), zone.id(), reader.zonePath(q.tenantId(), zone.id()), q.outletId(), q.terminalId(), q.sellerUserId(), agent.activeForSale(), warnings);
  }
}
