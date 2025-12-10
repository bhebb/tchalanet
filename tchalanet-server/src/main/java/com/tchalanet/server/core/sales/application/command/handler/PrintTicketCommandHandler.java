package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.application.port.in.PrintTicketUseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketPrinterPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class PrintTicketCommandHandler implements PrintTicketUseCase {

  private final TicketWritterPort ticketRepository;
  private final TicketPrinterPort ticketPrinterPort;

  @Override
  public String getPrintableTicket(UUID ticketId, UUID tenantId) {
    Ticket ticket =
        ticketRepository
            .findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    if (!ticket.getTenantId().equals(tenantId)) {
      throw new SecurityException("Tenant mismatch for ticket " + ticketId);
    }

    PrintTicketPayload payload = buildPayload(ticket);

    return ticketPrinterPort.render(payload);
  }

  private PrintTicketPayload buildPayload(Ticket ticket) {
    String tenantName = "Tenant " + ticket.getTenantId().toString().substring(0, 4);
    String terminalInfo = "Terminal " + ticket.getTerminalId().toString().substring(0, 4);
    String drawName = "Draw " + ticket.getDrawId().toString().substring(0, 4);
    Instant drawTime = Instant.now(); // Placeholder

    var lines =
        ticket.getLines().stream()
            .map(
                line ->
                    new PrintLine(
                        line.gameCode(),
                        line.selection(),
                        line.stake(),
                        line.potentialPayout()))
            .collect(Collectors.toList());

    return new PrintTicketPayload(
        tenantName,
        terminalInfo,
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getCreatedAt(),
        drawName,
        drawTime,
        ticket.getTotalAmount(),
        lines);
  }
}

