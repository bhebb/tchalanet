package com.tchalanet.server.platform.tenantgame.api.model;

import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record TenantGameSettingsUpdatedEvent(
    TenantGameId tenantGameId,
    TenantId tenantId,
    String gameCode,
    Instant occurredAt,
    String updatedBy
) {}
