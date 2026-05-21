package com.tchalanet.server.features.cashier.session.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OpenCashierSessionRequest(
    @NotNull OutletId outletId,
    @NotNull TerminalId terminalId,
    @NotNull @DecimalMin("0.00") BigDecimal openingFloat
) {}
