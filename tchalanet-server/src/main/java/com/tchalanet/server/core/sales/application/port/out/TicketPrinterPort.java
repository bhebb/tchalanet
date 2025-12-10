package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.core.sales.application.port.in.PrintTicketUseCase.PrintTicketPayload;

/**
 * Outbound Port for rendering a ticket's data into a specific printable format. Implementations
 * could format the payload as plain text, HTML, ESC/POS commands, etc.
 */
public interface TicketPrinterPort {
  String render(PrintTicketPayload payload);
}
