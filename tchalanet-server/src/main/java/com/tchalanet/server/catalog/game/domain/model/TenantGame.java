package com.tchalanet.server.catalog.game.domain.model;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantGameId;
import java.math.BigDecimal;
import java.util.Map;

public record TenantGame(
    TenantGameId tenantGameId,
    GameId gameId,
    String code,
    String name,
    String category,
    Integer minDigits,
    Integer maxDigits,
    String combination,
    Boolean enabled,
    String displayName,
    BigDecimal minStake,
    BigDecimal maxStake,
    Map<String, Object> flags) {}
