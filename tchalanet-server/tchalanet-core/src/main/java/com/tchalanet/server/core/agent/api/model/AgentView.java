package com.tchalanet.server.core.agent.api.model;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.List;

public record AgentView(
    AgentId id,
    AgentId parentAgentId,
    String displayName,
    AgentType type,
    AgentStatus status,
    AgentZoneId primaryZoneId,
    UserId ownerUserId,
    int depth,
    List<AgentMandateView> mandates
) {}
