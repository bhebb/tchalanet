package com.tchalanet.server.core.sales.infra.events;

import com.tchalanet.server.core.sales.domain.ports.out.TicketEventPublisherPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringTicketEventPublisherAdapter implements TicketEventPublisherPort {

  private final ApplicationEventPublisher eventPublisher;

  public void publishTicketCreatedEvent(UUID ticketId, UUID tenantId) {
    // This method signature needs to be updated to include sessionId
    // For now, we'll log a warning or throw an error if sessionId is not available.
    // The CreateTicketService will need to pass the sessionId.
    log.error(
        "publishTicketCreatedEvent called without sessionId. This method signature needs to be updated.");
    throw new UnsupportedOperationException(
        "publishTicketCreatedEvent must be called with sessionId.");
  }

  @Override
  public void publishTicketCreatedEvent(UUID ticketId, UUID tenantId, UUID sessionId) {
    log.info("Publishing TicketCreatedEvent for ticketId: {} (session: {})", ticketId, sessionId);
    TicketCreatedEvent event = new TicketCreatedEvent(this, ticketId, tenantId, sessionId);
    eventPublisher.publishEvent(event);
  }
}
