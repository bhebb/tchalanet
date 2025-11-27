package com.tchalanet.server.ticket.domain.ports.out;

import com.tchalanet.server.ticket.domain.model.Ticket;
import com.tchalanet.server.ticket.domain.ports.in.ListTicketsQuery.PageRequest;
import com.tchalanet.server.ticket.domain.ports.in.ListTicketsQuery.PagedResult;
import com.tchalanet.server.ticket.domain.ports.in.ListTicketsQuery.TicketFilter;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Outbound Port for ticket persistence. */
public interface TicketRepositoryPort {
  Ticket save(Ticket ticket);

  Optional<Ticket> findById(UUID ticketId);

  Optional<Ticket> findByPublicCode(String publicCode);

  PagedResult<Ticket> search(TicketFilter filter, PageRequest pageRequest);

  /**
   * Archives tickets created before the cutoff date for a specific tenant. This is a bulk operation
   * delegated to the persistence layer for efficiency.
   *
   * @param tenantId The tenant to process.
   * @param cutoffDate The cutoff timestamp.
   * @return The number of tickets archived.
   */
  int archiveOldTickets(UUID tenantId, Instant cutoffDate);
}
