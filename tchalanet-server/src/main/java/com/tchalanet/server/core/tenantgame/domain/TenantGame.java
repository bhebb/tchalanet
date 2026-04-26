package com.tchalanet.server.core.tenantgame.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TenantGameId;
import java.math.BigDecimal;
import java.util.Map;

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
