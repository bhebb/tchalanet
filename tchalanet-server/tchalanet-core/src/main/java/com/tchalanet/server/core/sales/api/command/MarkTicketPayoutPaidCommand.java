package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;

public record MarkTicketPayoutPaidCommand(
    TenantId tenantId,
    TicketId ticketId,
    UserId paidBy,
    String currency,
    String source
) implements Command<com.tchalanet.server.core.sales.api.command.MarkTicketPayoutPaidResult> {}

