package com.tchalanet.server.core.sales.api.command.print;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.document.api.model.PrintOptionsRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordTicketPrintCommand(
    @NotNull TicketId ticketId,
    @NotNull PrintOptionsRequest printOptionsRequest,
    @Size(max = 500) String reason,
    UserId actorUserId,
    CorrelationId correlationId
) implements Command<RecordTicketPrintResult> {}
