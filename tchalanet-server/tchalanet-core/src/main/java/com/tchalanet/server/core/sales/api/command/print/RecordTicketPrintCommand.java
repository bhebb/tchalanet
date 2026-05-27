package com.tchalanet.server.core.sales.api.command.print;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordTicketPrintCommand(
    @NotNull TicketId ticketId,
    @NotNull PrintOutputFormat format,
    @Size(max = 500) String reason,
    UserId actorUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    CorrelationId correlationId
) implements Command<RecordTicketPrintResult> {}
