package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.UUID;

/** Command to mark a ticket as paid. */
public record MarkTicketPayoutPaidCommand(
    TicketId ticketId,
    String reason,
    UUID performedBy,
    String currency)
    implements Command<Ticket> {}
