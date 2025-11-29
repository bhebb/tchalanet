package com.tchalanet.server.core.sales.domain.ports.in;

import com.tchalanet.server.core.sales.domain.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Inbound Port for querying a list of tickets. */
public interface ListTicketsQuery {

  PagedResult<TicketSummaryDto> search(TicketFilter filter, PageRequest pageRequest);

  record TicketFilter(
      UUID tenantId,
      UUID terminalId, // optional
      UUID drawId, // optional
      TicketStatus status, // optional
      Instant from, // optional
      Instant to // optional
      ) {}

  record PageRequest(
      int page, // 0-based
      int size) {}

  record PagedResult<T>(List<T> items, long totalItems, int totalPages, int currentPage) {}

  record TicketSummaryDto(
      UUID id,
      String ticketCode,
      String publicCode,
      TicketStatus status,
      BigDecimal totalAmount,
      Instant createdAt,
      String terminalLabel, // Example of resolved data
      String drawInfo // Example of resolved data
      ) {}

  // Detailed DTO used by the web mapper
  record TicketDetailsDto(
      UUID id,
      UUID tenantId,
      DrawRef draw,
      String ticketCode,
      String publicCode,
      TicketStatus status,
      BigDecimal totalAmount,
      Instant createdAt,
      List<TicketLineDto> lines) {
    public record DrawRef(UUID id) {}

    public record TicketLineDto(
        String gameCode, String selection, BigDecimal stake, BigDecimal potentialPayout) {}
  }
}
