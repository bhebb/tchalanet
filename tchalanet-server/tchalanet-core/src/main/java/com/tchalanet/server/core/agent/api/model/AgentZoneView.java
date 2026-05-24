package com.tchalanet.server.core.agent.api.model;

import com.tchalanet.server.common.types.id.AgentZoneId;

public record AgentZoneView(
    AgentZoneId id,
    AgentZoneId parentZoneId,
    String code,
    String name,
    String zoneType,
    AgentZoneStatus status,
    int depth
) {}
