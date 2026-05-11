package com.tchalanet.server.core.sales.application.command.model.lifecycle;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;

public record ApproveTicketSaleCommand(TicketId ticketId, UserId approvedBy, String reason)
    implements Command<com.tchalanet.server.core.sales.application.command.model.TicketApprovedResult> {}

