package com.tchalanet.server.platform.tenantgame.api.model;

import com.tchalanet.server.common.types.id.TenantGameId;

/**
 * Result model for DisableTenantGameRequest.
 * Maps to spec requirement TG1 (enable/disable commands).
 */
public record DisableTenantGameResult(
    TenantGameId tenantGameId
) {}
