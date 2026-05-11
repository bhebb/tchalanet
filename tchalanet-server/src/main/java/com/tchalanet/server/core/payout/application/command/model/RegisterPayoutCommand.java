package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;


public record RegisterPayoutCommand(
    @NotNull TenantId tenantId,
    @NotNull TicketId ticketId,
    @NotNull UserId requestedBy,
    SalesSessionId payingSessionId,
    OutletId payingOutletId,
    TerminalId terminalId,
    String reason
) implements Command<RegisterPayoutResult> {}
