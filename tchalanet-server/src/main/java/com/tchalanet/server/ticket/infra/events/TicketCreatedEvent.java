package com.tchalanet.server.ticket.infra.events;

import java.util.UUID;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TicketCreatedEvent extends ApplicationEvent {
  private final UUID ticketId;
  private final UUID tenantId;
  private final UUID sessionId; // Added sessionId

  public TicketCreatedEvent(Object source, UUID ticketId, UUID tenantId, UUID sessionId) {
    super(source);
    this.ticketId = ticketId;
    this.tenantId = tenantId;
    this.sessionId = sessionId;
  }
}
