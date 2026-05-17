package com.tchalanet.server.core.sales.api.command.print;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintState;

public record RecordTicketPrintResult(
    TicketId ticketId,
    TicketPrintState printState
) {}
