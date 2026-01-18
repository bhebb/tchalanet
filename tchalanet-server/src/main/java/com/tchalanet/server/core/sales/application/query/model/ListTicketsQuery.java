package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.paging.TchPage;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Query to list tickets. */
public record ListTicketsQuery(TicketFilter filter, Pageable pageRequest)
    implements Query<TchPage<ListTicketsQuery.TicketSummaryDto>> {

  /** Filter for tickets. */
  public record TicketFilter(
      TenantId tenantId,
      TerminalId terminalId, // optional
      DrawId drawId, // optional
      TicketStatus status, // optional
      Instant from, // optional
      Instant to // optional
      ) {}

  /** Paged result. */

  /** DTO for ticket summary. */
  public record TicketSummaryDto(
      TicketId id,
      String ticketCode,
      String publicCode,
      TicketStatus status,
      BigDecimal totalAmount,
      Instant createdAt,
      String terminalLabel, // Example of resolved data
      String drawInfo // Example of resolved data
      ) {}

  /** DTO for ticket details. */
  public record TicketDetailsDto(
      com.tchalanet.server.common.types.id.TicketId id,
      TenantId tenantId,
      DrawRef draw,
      String ticketCode,
      String publicCode,
      TicketStatus status,
      BigDecimal totalAmount,
      Instant createdAt,
      List<TicketLineDto> lines) {

    /** Reference to draw. */
    public record DrawRef(com.tchalanet.server.common.types.id.DrawId id) {}

    /** DTO for ticket line. */
    public record TicketLineDto(
        String gameCode, String selection, BigDecimal stake, BigDecimal potentialPayout) {}
  }
}
