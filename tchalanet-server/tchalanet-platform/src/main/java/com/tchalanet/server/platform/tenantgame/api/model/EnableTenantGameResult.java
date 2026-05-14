package com.tchalanet.server.platform.tenantgame.api.model;

import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.GameId;

/**
 * Result model for EnableTenantGameRequest.
 * Maps to spec requirement TG1 (enable/disable commands).
 */
public record EnableTenantGameResult(
    TenantGameId tenantGameId,
    GameId gameId,
    String gameCode,
    Boolean enabled
) {}
