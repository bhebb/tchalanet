package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.core.sales.domain.model.Ticket;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound Port for ticket persistence.
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
}
