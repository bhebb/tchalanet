package com.tchalanet.server.platform.tenantgame.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TenantGameId;
import java.time.Instant;
import java.util.Map;

/**
 * Domain event published after tenant game policy is updated.
 * Maps to spec requirement TG3 & TG5 (policies & events).
 */
public record TenantGamePolicyUpdatedEvent(
    TenantGameId tenantGameId,
    TenantId tenantId,
    String gameCode,
    Map<String, Object> policyUpdates,
    Instant timestamp,
    String initiator
) {}
