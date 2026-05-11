package com.tchalanet.server.core.payout.infra.web.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import jakarta.validation.constraints.NotNull;

public record ExecutePayoutRequest(
    @NotNull SalesSessionId payingSessionId,
    @NotNull OutletId payingOutletId,
    @NotNull TerminalId terminalId,
    String reason) {
}
