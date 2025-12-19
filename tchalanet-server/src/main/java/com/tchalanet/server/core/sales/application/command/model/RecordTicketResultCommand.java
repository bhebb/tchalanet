package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.time.Instant;
import java.util.UUID;

public record RecordTicketResultCommand(UUID tenantId, UUID ticketId, Instant resultedAt) implements Command<Ticket> {
}

