package com.tchalanet.server.core.pricing.internal.domain;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerTerminalOddsOverride(
    SellerTerminalOddsOverrideId id,
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    PayoutRuleType payoutRuleType,
    BigDecimal fixedAmount,
    boolean active,
    Instant effectiveFrom,
    Instant effectiveTo,
    String reason,
    Instant createdAt,
    UserId createdBy,
    Instant updatedAt,
    UserId updatedBy,
    Instant deletedAt
) {
    public SellerTerminalOddsOverride {
        payoutRuleType = payoutRuleType == null ? PayoutRuleType.STAKE_MULTIPLIER : payoutRuleType;
        if (payoutRuleType == PayoutRuleType.FIXED_AMOUNT
            && (fixedAmount == null || fixedAmount.signum() < 0)) {
            throw new IllegalArgumentException("fixedAmount must be non-negative");
        }
        if (payoutRuleType == PayoutRuleType.STAKE_MULTIPLIER) {
            fixedAmount = null;
        }
    }

    public SellerTerminalOddsOverride(
        SellerTerminalOddsOverrideId id,
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        PricingVariantCode pricingVariantCode,
        String betType,
        Short betOption,
        BigDecimal odds,
        boolean active,
        Instant effectiveFrom,
        Instant effectiveTo,
        String reason,
        Instant createdAt,
        UserId createdBy,
        Instant updatedAt,
        UserId updatedBy,
        Instant deletedAt
    ) {
        this(
            id,
            tenantId,
            sellerTerminalId,
            gameCode,
            pricingVariantCode,
            betType,
            betOption,
            odds,
            PayoutRuleType.STAKE_MULTIPLIER,
            null,
            active,
            effectiveFrom,
            effectiveTo,
            reason,
            createdAt,
            createdBy,
            updatedAt,
            updatedBy,
            deletedAt
        );
    }

    public static SellerTerminalOddsOverride createNew(
        SellerTerminalOddsOverrideId id,
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        PricingVariantCode pricingVariantCode,
        String betType,
        Short betOption,
        BigDecimal odds,
        Instant effectiveFrom,
        Instant effectiveTo,
        String reason,
        UserId actorId
    ) {
        var now = Instant.now();
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, pricingVariantCode, betType, betOption,
            odds, PayoutRuleType.STAKE_MULTIPLIER, null, true,
            effectiveFrom, effectiveTo, reason,
            now, actorId, now, actorId, null);
    }

    public static SellerTerminalOddsOverride createNew(
        SellerTerminalOddsOverrideId id,
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        PricingVariantCode pricingVariantCode,
        String betType,
        Short betOption,
        BigDecimal odds,
        PayoutRuleType payoutRuleType,
        BigDecimal fixedAmount,
        Instant effectiveFrom,
        Instant effectiveTo,
        String reason,
        UserId actorId
    ) {
        var now = Instant.now();
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, pricingVariantCode, betType, betOption,
            odds, payoutRuleType, fixedAmount, true,
            effectiveFrom, effectiveTo, reason,
            now, actorId, now, actorId, null);
    }

    public SellerTerminalOddsOverride update(
        BigDecimal newOdds,
        Instant effectiveFrom,
        Instant effectiveTo,
        String reason,
        UserId actorId
    ) {
        return update(newOdds, PayoutRuleType.STAKE_MULTIPLIER, null, effectiveFrom, effectiveTo, reason, actorId);
    }

    public SellerTerminalOddsOverride update(
        BigDecimal newOdds,
        PayoutRuleType newPayoutRuleType,
        BigDecimal newFixedAmount,
        Instant effectiveFrom,
        Instant effectiveTo,
        String reason,
        UserId actorId
    ) {
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, pricingVariantCode, betType, betOption,
            newOdds, newPayoutRuleType, newFixedAmount, true,
            effectiveFrom, effectiveTo, reason,
            createdAt, createdBy, Instant.now(), actorId, null);
    }

    public SellerTerminalOddsOverride deactivate(UserId actorId) {
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, pricingVariantCode, betType, betOption,
            odds, payoutRuleType, fixedAmount, false,
            effectiveFrom, effectiveTo, reason,
            createdAt, createdBy, Instant.now(), actorId, null);
    }

    public SellerTerminalOddsOverride softDelete(UserId actorId) {
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, pricingVariantCode, betType, betOption,
            odds, payoutRuleType, fixedAmount, false,
            effectiveFrom, effectiveTo, reason,
            createdAt, createdBy, Instant.now(), actorId, Instant.now());
    }
}
