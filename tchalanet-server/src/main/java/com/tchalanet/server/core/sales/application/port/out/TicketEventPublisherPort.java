package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;

/** Outbound Port for publishing ticket-related domain events. */
public interface TicketEventPublisherPort {
  void publishTicketPlacedEvent(TicketPlacedEvent event);
}
