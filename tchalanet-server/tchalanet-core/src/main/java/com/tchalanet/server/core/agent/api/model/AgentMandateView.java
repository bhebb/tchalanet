package com.tchalanet.server.core.agent.api.model;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;

public record AgentMandateView(
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
) {}
