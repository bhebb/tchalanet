package com.tchalanet.server.core.pricing.internal.infra.web.admin.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record UpsertOddsOverrideRequest(
    @NotBlank String gameCode,
    @NotBlank String betType,
    Short betOption,
    @NotNull @DecimalMin(value = "0.0001", inclusive = false) BigDecimal odds,
    Instant effectiveFrom,
    Instant effectiveTo,
    String reason
) {}
