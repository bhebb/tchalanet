package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.application.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OverrideTicketResultCommand(
    TicketId ticketId,
    BigDecimal totalPayout,
    TicketStatus status, // RESULTED_WON ou RESULTED_LOST
    String reason,
    UUID performedBy,
    Instant performedAt)
    implements Command<Void> {}
