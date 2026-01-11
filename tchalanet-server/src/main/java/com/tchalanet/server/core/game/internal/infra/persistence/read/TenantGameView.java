package com.tchalanet.server.core.game.internal.infra.persistence.read;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
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
    JsonNode flags) {}
