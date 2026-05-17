package com.tchalanet.server.core.sales.internal.infra.web.model;

import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Request to override a ticket result")
public record OverrideTicketResultRequest(
    @NotNull(message = "Result status is required")
    @Schema(description = "Forced result status", example = "WON")
    TicketResultStatus status,

    @NotNull(message = "Total payout is required")
    @PositiveOrZero(message = "Total payout must be >= 0")
    @Schema(description = "Forced total payout amount", example = "1250.00")
    BigDecimal totalPayout,

    @NotBlank(message = "Override reason is required")
    @Schema(description = "Reason for override", example = "Manual correction after incident")
    String reason,

    @Schema(description = "Override timestamp; if omitted, server clock is used")
    Instant performedAt
) {}

