package com.tchalanet.server.core.agent.internal.infra.persistence;

import java.time.Instant;
import java.util.UUID;

public record AgentSummaryProjection(UUID id, String displayName, String type, String status, Instant createdAt) {}

