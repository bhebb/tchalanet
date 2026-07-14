package com.tchalanet.server.core.pricing.internal.infra.web.admin.model;

import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeleteTenantPricingRuleRequest(
    @NotBlank String gameCode, @NotNull PricingVariantCode pricingVariantCode) {}
