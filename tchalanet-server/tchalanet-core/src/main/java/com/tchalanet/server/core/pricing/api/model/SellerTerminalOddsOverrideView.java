package com.tchalanet.server.core.pricing.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerTerminalOddsOverrideView(
    SellerTerminalOddsOverrideId id,
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String gameCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    boolean active,
    Instant effectiveFrom,
    Instant effectiveTo,
    String reason,
    Instant createdAt,
    Instant updatedAt
) {}
