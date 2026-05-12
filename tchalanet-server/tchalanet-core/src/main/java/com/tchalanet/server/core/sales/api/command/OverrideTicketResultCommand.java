package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.application.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OverrideTicketResultCommand(
    TicketId ticketId,
    BigDecimal totalPayout,
    TicketStatus status, // RESULTED_WON ou RESULTED_LOST
    String reason,
    UserId performedBy,
    Instant performedAt)
    implements Command<Void> {}
