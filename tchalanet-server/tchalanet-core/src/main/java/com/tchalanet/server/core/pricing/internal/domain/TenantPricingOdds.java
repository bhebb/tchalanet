package com.tchalanet.server.core.pricing.internal.domain;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
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
    PayoutRuleType payoutRuleType,
    BigDecimal fixedAmount,
    boolean active,
    Instant deletedAt) {
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
    payoutRuleType = payoutRuleType == null ? PayoutRuleType.STAKE_MULTIPLIER : payoutRuleType;
    if (payoutRuleType == PayoutRuleType.FIXED_AMOUNT
        && (fixedAmount == null || fixedAmount.signum() < 0)) {
      throw new IllegalArgumentException("fixedAmount must be non-negative");
    }
    if (payoutRuleType == PayoutRuleType.STAKE_MULTIPLIER) {
      fixedAmount = null;
    }
  }

  public TenantPricingOdds(
      TenantId tenantId,
      String gameCode,
      PricingVariantCode pricingVariantCode,
      String betType,
      Short betOption,
      BigDecimal odds,
      boolean active,
      Instant deletedAt) {
    this(
        tenantId,
        gameCode,
        pricingVariantCode,
        betType,
        betOption,
        odds,
        PayoutRuleType.STAKE_MULTIPLIER,
        null,
        active,
        deletedAt);
  }

  public TenantPricingOdds update(BigDecimal newOdds, String newBetType, Short newBetOption) {
    return update(newOdds, newBetType, newBetOption, PayoutRuleType.STAKE_MULTIPLIER, null);
  }

  public TenantPricingOdds update(
      BigDecimal newOdds,
      String newBetType,
      Short newBetOption,
      PayoutRuleType newPayoutRuleType,
      BigDecimal newFixedAmount) {
    return new TenantPricingOdds(
        tenantId,
        gameCode,
        pricingVariantCode,
        normalizeBetType(newBetType),
        newBetOption,
        newOdds,
        newPayoutRuleType,
        newFixedAmount,
        true,
        null);
  }

  public TenantPricingOdds softDelete() {
    return new TenantPricingOdds(
        tenantId,
        gameCode,
        pricingVariantCode,
        betType,
        betOption,
        odds,
        payoutRuleType,
        fixedAmount,
        false,
        null);
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
