package com.tchalanet.server.features.cashier.session.model;

import com.tchalanet.server.common.types.id.SalesSessionId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CloseCashierSessionRequest(
    @NotNull SalesSessionId sessionId,
    @NotNull @DecimalMin("0.00") BigDecimal closingAmount,
    @NotBlank String reason
) {}
