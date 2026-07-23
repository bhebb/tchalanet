package com.tchalanet.server.features.pos.profile.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePosProfileCommercialRequest(
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal commissionRate) {}
