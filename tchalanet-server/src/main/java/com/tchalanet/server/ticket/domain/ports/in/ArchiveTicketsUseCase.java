package com.tchalanet.server.ticket.domain.ports.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound Port for archiving old tickets. This is typically a system-level maintenance operation.
 */
public interface ArchiveTicketsUseCase {

  /**
   * Archives tickets for a specific tenant that were created before a given cutoff date.
   *
   * @param tenantId The ID of the tenant whose tickets should be archived.
   * @param cutoffDate The timestamp before which tickets will be archived.
   * @return The number of tickets that were archived.
   */
  int archiveTickets(UUID tenantId, Instant cutoffDate);
}
