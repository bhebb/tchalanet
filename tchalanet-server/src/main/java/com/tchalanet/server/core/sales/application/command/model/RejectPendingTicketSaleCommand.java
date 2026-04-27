package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import java.util.UUID;

public record RejectPendingTicketSaleCommand(
    TicketId ticketId,
    UUID approvalRequestId,
    UUID rejectedBy,
    String reason)
    implements Command<Void> {}

