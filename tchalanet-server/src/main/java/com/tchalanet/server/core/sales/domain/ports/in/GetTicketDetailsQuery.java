package com.tchalanet.server.core.sales.domain.ports.in;

import com.tchalanet.server.core.sales.domain.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Inbound Port for fetching the full details of a single ticket. */
public interface GetTicketDetailsQuery {

  Optional<TicketDetailsDto> findByPublicCode(String publicCode);

  Optional<TicketDetailsDto> findById(UUID id);

  record TicketDetailsDto(
      UUID id,
      UUID tenantId,
      String ticketCode,
      String publicCode,
      TicketStatus status,
      BigDecimal totalAmount,
      Instant createdAt,
      DrawInfo draw,
      List<LineInfo> lines) {}

  record DrawInfo(UUID id, String name, Instant scheduledAt) {}

  record LineInfo(
      String gameCode, String selection, BigDecimal stake, BigDecimal potentialPayout) {}
}
