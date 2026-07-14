package com.tchalanet.server.core.pricing.internal.infra.web.admin.model;

import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record UpsertPricingRuleOverrideRequest(
    @NotBlank String gameCode,
    @NotNull PricingVariantCode pricingVariantCode,
    @NotBlank String betType,
    Short betOption,
    @DecimalMin(value = "0.0001", inclusive = false) BigDecimal odds,
    PayoutRuleType payoutRuleType,
    @DecimalMin(value = "0", inclusive = true) BigDecimal fixedAmount,
    Instant effectiveFrom,
    Instant effectiveTo,
    String reason) {}
