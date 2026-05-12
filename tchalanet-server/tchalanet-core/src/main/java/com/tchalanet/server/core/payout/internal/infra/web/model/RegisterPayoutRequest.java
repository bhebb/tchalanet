package com.tchalanet.server.core.payout.internal.infra.web.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import jakarta.validation.constraints.NotNull;

public record RegisterPayoutRequest(
    @NotNull TicketId ticketId,
    @NotNull OutletId payingOutletId,
    @NotNull SalesSessionId payingSessionId,
    @NotNull TerminalId terminalId,
    String reason
) {}

