package com.tchalanet.server.ticket.domain.ports.out;

import java.util.UUID;

/** Outbound Port for publishing ticket-related domain events. */
public interface TicketEventPublisherPort {
  void publishTicketCreatedEvent(UUID ticketId, UUID tenantId, UUID sessionId);
}
