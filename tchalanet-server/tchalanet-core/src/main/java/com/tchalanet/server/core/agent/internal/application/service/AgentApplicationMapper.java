package com.tchalanet.server.core.agent.internal.application.service;

import com.tchalanet.server.core.agent.api.model.*;
import com.tchalanet.server.core.agent.internal.domain.model.*;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public class AgentApplicationMapper {
  public AgentZoneView toZoneView(AgentZone z) {
    return new AgentZoneView(z.id(), z.parentZoneId(), z.code(), z.name(), z.zoneType(), z.status(), z.depth());
  }
  public AgentMandateView toMandateView(AgentZoneMandate m) {
    return new AgentMandateView(m.agentId(), m.zoneId(), m.canSell(), m.canCreateSubAgents(), m.canCreateSellers(), m.canCreateOutlets(), m.canManageTerminals(), m.canViewReports(), m.maxChildAgentDepth(), m.maxChildAgents(), m.maxSellers(), m.maxTerminals());
  }
  public AgentView toAgentView(Agent a) {
    return new AgentView(a.id(), a.parentAgentId(), a.displayName(), a.type(), a.status(), a.primaryZoneId(), a.ownerUserId(), a.depth(), a.mandates().stream().map(this::toMandateView).toList());
  }
}
