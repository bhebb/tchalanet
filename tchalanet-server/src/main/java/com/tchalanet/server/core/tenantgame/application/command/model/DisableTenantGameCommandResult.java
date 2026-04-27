package com.tchalanet.server.core.tenantgame.application.command.model;

import com.tchalanet.server.common.types.id.TenantGameId;

/**
 * Result model for DisableTenantGameCommand.
 * Maps to spec requirement TG1 (enable/disable commands).
 */
public record DisableTenantGameCommandResult(
    TenantGameId tenantGameId
) {}
