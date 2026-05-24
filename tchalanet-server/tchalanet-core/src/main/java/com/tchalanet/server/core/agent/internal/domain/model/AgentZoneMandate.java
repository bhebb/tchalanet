package com.tchalanet.server.core.agent.internal.domain.model;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;

public record AgentZoneMandate(
    AgentId agentId,
    AgentZoneId zoneId,
    boolean canSell,
    boolean canCreateSubAgents,
    boolean canCreateSellers,
    boolean canCreateOutlets,
    boolean canManageTerminals,
    boolean canViewReports,
    int maxChildAgentDepth,
    int maxChildAgents,
    int maxSellers,
    int maxTerminals
) {
  public AgentZoneMandate {
    if (agentId == null) throw new IllegalArgumentException("agent_mandate.agent_required");
    if (zoneId == null) throw new IllegalArgumentException("agent_mandate.zone_required");
    if (maxChildAgentDepth < 0) throw new IllegalArgumentException("agent_mandate.invalid_depth");
  }

  public boolean canDelegate() { return canCreateSubAgents && maxChildAgentDepth > 0; }

  public static AgentZoneMandate defaultCommercialMandate(AgentId agentId, AgentZoneId zoneId, int depth, int maxV1Depth) {
    boolean canSell = true;
    boolean canCreateSubAgents = depth < maxV1Depth;
    boolean canCreateSellers = true;
    boolean canCreateOutlets = true;
    boolean canManageTerminals = true;
    boolean canViewReports = true;
    int maxChildAgentDepth = Math.max(0, maxV1Depth - depth);
    int maxChildAgents = 100;
    int maxSellers = 1000;
    int maxTerminals = 1000;
    return new AgentZoneMandate(
        agentId,
        zoneId,
        canSell,
        canCreateSubAgents,
        canCreateSellers,
        canCreateOutlets,
        canManageTerminals,
        canViewReports,
        maxChildAgentDepth,
        maxChildAgents,
        maxSellers,
        maxTerminals
    );
  }
}
