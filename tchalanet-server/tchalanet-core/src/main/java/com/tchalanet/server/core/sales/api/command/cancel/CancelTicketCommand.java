package com.tchalanet.server.core.sales.api.command.cancel;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelTicketCommand(
    @NotNull TicketId ticketId,
    @Size(max = 500) String reason
) implements Command<CancelTicketResult> {
}
