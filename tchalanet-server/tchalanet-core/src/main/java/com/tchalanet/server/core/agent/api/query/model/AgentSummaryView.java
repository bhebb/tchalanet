package com.tchalanet.server.core.agent.api.query.model;

import com.tchalanet.server.common.types.id.AgentId;

import java.time.Instant;

public record AgentSummaryView(
    AgentId id,
    String displayName,
    String type,
    String status,
    Instant createdAt
) {}

