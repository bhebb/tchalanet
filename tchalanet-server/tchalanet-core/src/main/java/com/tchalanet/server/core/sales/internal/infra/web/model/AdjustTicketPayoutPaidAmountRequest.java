package com.tchalanet.server.core.sales.internal.infra.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AdjustTicketPayoutPaidAmountRequest(
    @NotNull(message = "Previous paid amount is required")
        @PositiveOrZero(message = "Previous paid amount must be >= 0")
        @Schema(description = "Current paid amount before adjustment", example = "1250.00")
        BigDecimal previousPaidAmount,
    @NotNull(message = "Adjusted paid amount is required")
        @PositiveOrZero(message = "Adjusted paid amount must be >= 0")
        @Schema(description = "Corrected paid amount after adjustment", example = "1200.00")
        BigDecimal paidAmount,
    @NotBlank(message = "Reason is required")
        @Schema(description = "Audit reason for the paid amount adjustment")
        String reason) {}
