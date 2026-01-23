package com.tchalanet.server.catalog.pricing.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Read-only view of a pricing odds entry */
public record PricingView(
    UUID id,
    UUID tenantId,
    String gameCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
