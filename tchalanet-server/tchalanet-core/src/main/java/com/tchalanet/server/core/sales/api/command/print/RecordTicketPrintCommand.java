package com.tchalanet.server.core.sales.api.command.print;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordTicketPrintCommand(
    @NotNull TicketId ticketId,
    @NotNull PrintOutputFormat format,
    @Size(max = 500) String reason
) implements Command<RecordTicketPrintResult> {}
