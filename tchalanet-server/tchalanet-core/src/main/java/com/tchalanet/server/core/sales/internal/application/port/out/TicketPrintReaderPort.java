package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import jakarta.validation.constraints.NotNull;

public interface TicketPrintReaderPort {

    TicketPrintView findPrintViewRequired(@NotNull TicketId ticketId);
}
