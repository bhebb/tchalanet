package com.tchalanet.server.platform.tenantgame.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TenantGameId;
import java.time.Instant;

/**
 * Domain event published after tenant game is disabled.
 * Maps to spec requirement TG5 (events & idempotence).
 */
public record TenantGameDisabledEvent(
    TenantGameId tenantGameId,
    TenantId tenantId,
    String gameCode,
    Instant timestamp,
    String initiator
) {}
