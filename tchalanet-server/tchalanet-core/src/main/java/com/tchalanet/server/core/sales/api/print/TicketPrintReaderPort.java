package com.tchalanet.server.core.sales.api.print;

import com.tchalanet.server.common.types.id.TicketId;
import java.util.Locale;
import java.util.Optional;

public interface TicketPrintReaderPort {
  Optional<TicketPrintView> findTicketPrintView(TicketId ticketId, Locale locale);
}
