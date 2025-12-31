package com.tchalanet.server.core.game.infra.web.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TenantGameUpdateRequest(
    Boolean enabled,
    @Size(max = 128) String displayName,
    BigDecimal minStake,
    BigDecimal maxStake,
    JsonNode flags) {}
