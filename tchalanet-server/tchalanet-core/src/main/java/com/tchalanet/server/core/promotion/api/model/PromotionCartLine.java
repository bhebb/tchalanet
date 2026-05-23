package com.tchalanet.server.core.promotion.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;


public record PromotionCartLine(
    @NotBlank String clientLineRef,
    @NotBlank String gameCode,
    @NotNull BigDecimal paidAmount,
    @NotNull BigDecimal effectiveStakeAmount
) {
}
