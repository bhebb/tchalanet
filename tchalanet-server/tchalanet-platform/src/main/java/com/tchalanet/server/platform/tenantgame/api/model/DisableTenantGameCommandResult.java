package com.tchalanet.server.platform.tenantgame.api.model;

import com.tchalanet.server.common.types.id.TenantGameId;

/**
 * Result model for DisableTenantGameCommand.
 * Maps to spec requirement TG1 (enable/disable commands).
 */
public record DisableTenantGameCommandResult(
    TenantGameId tenantGameId
) {}
