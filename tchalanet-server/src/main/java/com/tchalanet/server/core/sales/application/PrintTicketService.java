package com.tchalanet.server.core.sales.application;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.ports.in.PrintTicketUseCase;
import com.tchalanet.server.core.sales.domain.ports.out.TicketPrinterPort;
import com.tchalanet.server.core.sales.domain.ports.out.TicketRepositoryPort;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrintTicketService implements PrintTicketUseCase {

  private final TicketRepositoryPort ticketRepository;
  private final TicketPrinterPort ticketPrinterPort;

  // private final TenantReadModelPort tenantReadModel; // To get tenant name
  // private final TerminalReadModelPort terminalReadModel; // To get terminal info
  // private final DrawReadModelPort drawReadModel; // To get draw name and time

  @Override
  public String getPrintableTicket(UUID ticketId, UUID tenantId) {
    // 1. Find and authorize the ticket
    Ticket ticket =
        ticketRepository
            .findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    if (!ticket.getTenantId().equals(tenantId)) {
      throw new SecurityException("Tenant mismatch for ticket " + ticketId);
    }

    // 2. Build the data payload
    PrintTicketPayload payload = buildPayload(ticket);

    // 3. Delegate rendering to the outbound port
    return ticketPrinterPort.render(payload);
  }

  private PrintTicketPayload buildPayload(Ticket ticket) {
    // In a real implementation, this data would be fetched from dedicated read model ports
    String tenantName = "Tenant " + ticket.getTenantId().toString().substring(0, 4);
    String terminalInfo = "Terminal " + ticket.getTerminalId().toString().substring(0, 4);
    String drawName = "Draw " + ticket.getDrawId().toString().substring(0, 4);
    Instant drawTime = Instant.now(); // Placeholder

    var lines =
        ticket.getLines().stream()
            .map(
                line ->
                    new PrintLine(
                        line.gameCode(), // Placeholder for resolved game label
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
