package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.UUID;

/** Command to mark a ticket as paid. */
public record MarkTicketPaidCommand(TicketId ticketId, String reason, UUID performedBy)
    implements Command<Ticket> {}
