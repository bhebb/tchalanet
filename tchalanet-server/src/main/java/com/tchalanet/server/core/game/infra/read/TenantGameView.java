package com.tchalanet.server.core.game.infra.read;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record TenantGameView(
    UUID tenantGameId,
    UUID gameId,
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
