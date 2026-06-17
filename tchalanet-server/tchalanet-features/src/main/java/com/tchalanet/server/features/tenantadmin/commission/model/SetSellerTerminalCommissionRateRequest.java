package com.tchalanet.server.features.tenantadmin.commission.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetSellerTerminalCommissionRateRequest(
    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @DecimalMax(value = "100.00", inclusive = true)
    BigDecimal rate
) {}
