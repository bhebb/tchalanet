package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;

public record RejectTicketSaleCommand(TicketId ticketId, UserId rejectedBy, String reason)
    implements Command<com.tchalanet.server.core.sales.application.command.model.TicketRejectedResult> {}

