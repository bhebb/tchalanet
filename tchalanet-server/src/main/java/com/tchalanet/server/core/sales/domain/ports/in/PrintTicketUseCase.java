package com.tchalanet.server.core.sales.domain.ports.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Inbound Port for generating a printable representation of a ticket. */
public interface PrintTicketUseCase {

  /**
   * Fetches ticket data and renders it into a string format using a configured printer port.
   *
   * @param ticketId The ID of the ticket to print.
   * @param tenantId The ID of the tenant requesting the print, for authorization.
   * @return A string containing the formatted ticket content (e.g., plain text, HTML).
   */
  String getPrintableTicket(UUID ticketId, UUID tenantId);

  /**
   * A DTO containing all the necessary information to print a ticket. This object is constructed by
   * the use case and passed to the TicketPrinterPort.
   */
  record PrintTicketPayload(
      String tenantName,
      String terminalInfo,
      String ticketCode,
      String publicCode,
      Instant createdAt,
      String drawName,
      Instant drawTime,
      BigDecimal totalAmount,
      List<PrintLine> lines) {}

  record PrintLine(
      String gameLabel, String selection, BigDecimal stake, BigDecimal potentialPayout) {}
}
