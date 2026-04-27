package com.tchalanet.server.core.tenantgame.domain;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TenantGameId;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record TenantGame(
    TenantGameId tenantGameId,
    TenantId tenantId,
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
    JsonNode flags) {}
