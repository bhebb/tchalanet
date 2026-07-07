package com.tchalanet.server.core.pricing.internal.domain;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import java.math.BigDecimal;
import java.time.Instant;

public record TenantPricingOdds(
    TenantId tenantId,
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    boolean active,
    Instant deletedAt
) {
    public TenantPricingOdds {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (gameCode == null || gameCode.isBlank()) {
            throw new IllegalArgumentException("gameCode is required");
        }
        if (pricingVariantCode == null) {
            throw new IllegalArgumentException("pricingVariantCode is required");
        }
        if (betType == null || betType.isBlank()) {
            throw new IllegalArgumentException("betType is required");
        }
        if (odds == null || odds.signum() <= 0) {
            throw new IllegalArgumentException("odds must be positive");
        }
    }

    public TenantPricingOdds update(BigDecimal newOdds, String newBetType, Short newBetOption) {
        return new TenantPricingOdds(
            tenantId,
            gameCode,
            pricingVariantCode,
            normalizeBetType(newBetType),
            newBetOption,
            newOdds,
            true,
            null
        );
    }

    public TenantPricingOdds softDelete() {
        return new TenantPricingOdds(
            tenantId,
            gameCode,
            pricingVariantCode,
            betType,
            betOption,
            odds,
            false,
            null
        );
    }

    public static String normalizeGameCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("gameCode is required");
        }
        return raw.trim().toUpperCase();
    }

    public static String normalizeBetType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("betType is required");
        }
        return raw.trim().toUpperCase();
    }
}
