package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PageRequest;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketFilter;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound Port for ticket persistence (writes + common reads used by other cores).
 */
public interface TicketWritterPort {
    Ticket save(Ticket ticket);

    /**
     * Archives tickets created before the cutoff date for a specific tenant. This is a bulk operation
     * delegated to the persistence layer for efficiency.
     *
     * @param tenantId   The tenant to process.
     * @param cutoffDate The cutoff timestamp.
     * @return The number of tickets archived.
     */
    int archiveOldTickets(UUID tenantId, Instant cutoffDate);

    // Common read methods exposed by writer adapter for other core modules (e.g., payout)
    Optional<Ticket> findById(UUID ticketId);

    Optional<Ticket> findByPublicCode(String publicCode);

    PagedResult<Ticket> search(TicketFilter filter, PageRequest pageRequest);
}
