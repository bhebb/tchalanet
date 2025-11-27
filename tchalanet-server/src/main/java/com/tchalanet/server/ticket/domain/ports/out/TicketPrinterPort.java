package com.tchalanet.server.ticket.domain.ports.out;

import com.tchalanet.server.ticket.domain.ports.in.PrintTicketUseCase.PrintTicketPayload;

/**
 * Outbound Port for rendering a ticket's data into a specific printable format. Implementations
 * could format the payload as plain text, HTML, ESC/POS commands, etc.
 */
public interface TicketPrinterPort {
  String render(PrintTicketPayload payload);
}
