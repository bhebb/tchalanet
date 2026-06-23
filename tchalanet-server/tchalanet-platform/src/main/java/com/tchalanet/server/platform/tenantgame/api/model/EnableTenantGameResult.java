package com.tchalanet.server.platform.tenantgame.api.model;

import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.GameId;

import java.math.BigDecimal;

public record EnableTenantGameResult(
    TenantGameId tenantGameId,
    GameId gameId,
    String gameCode,
    Boolean enabled,
    Boolean visibleInPos,
    String displayName,
    Integer displayOrder,
    BigDecimal minStake,
    BigDecimal maxStake
) {}
