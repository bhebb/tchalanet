package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;

public record RegisterPayoutCommand(
    TenantId tenantId,
    TicketId ticketId,
    OutletId payingOutletId,
    SessionId payingSessionId,
    TerminalId terminalId,
    UserId paidBy,
    String reason)
    implements Command<RegisterPayoutResult> {}
