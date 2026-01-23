package com.tchalanet.server.catalog.pricing.internal.infra.web.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PricingOddsResponse(
    UUID id,
    UUID tenantId,
    String gameCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
