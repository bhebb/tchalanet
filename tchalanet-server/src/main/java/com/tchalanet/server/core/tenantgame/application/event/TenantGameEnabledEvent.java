package com.tchalanet.server.core.tenantgame.application.event;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TenantGameId;
import java.time.Instant;

/**
 * Domain event published after tenant game is enabled.
 * Maps to spec requirement TG5 (events & idempotence).
 */
public record TenantGameEnabledEvent(
    TenantGameId tenantGameId,
    TenantId tenantId,
    String gameCode,
    Instant timestamp,
    String initiator
) {}
