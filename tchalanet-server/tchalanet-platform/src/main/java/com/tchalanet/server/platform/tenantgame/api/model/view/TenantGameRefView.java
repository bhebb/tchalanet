package com.tchalanet.server.platform.tenantgame.api.model.view;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantGameId;

import java.math.BigDecimal;

public record TenantGameRefView(
    TenantGameId tenantGameId,
    GameId gameId,
    String gameCode,
    boolean enabled,
    boolean visibleInPos,
    String displayName,
    int displayOrder,
    BigDecimal minStake,
    BigDecimal maxStake
) {}
