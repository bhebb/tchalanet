package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.common.types.id.TicketId;
import java.util.Locale;

public interface TicketPrintReaderPort {
  TicketPrintView getTicketPrintView(TicketId ticketId, Locale locale);
}
