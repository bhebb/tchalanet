package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.UUID;

/** Command to mark a ticket as payment pending. */
public record MarkTicketPayoutPendingCommand(TicketId ticketId, String reason, UUID performedBy)
    implements Command<Ticket> {}
