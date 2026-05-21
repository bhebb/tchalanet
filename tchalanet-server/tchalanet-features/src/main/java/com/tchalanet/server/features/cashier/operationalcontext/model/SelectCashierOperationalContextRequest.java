package com.tchalanet.server.features.cashier.operationalcontext.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import jakarta.validation.constraints.NotNull;

public record SelectCashierOperationalContextRequest(
    @NotNull TerminalId terminalId,
    @NotNull OutletId outletId,
    @NotNull SalesSessionId salesSessionId
) {}
