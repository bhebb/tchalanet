package com.tchalanet.server.pos.infra.events;

import java.util.UUID;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SessionClosedEvent extends ApplicationEvent {
  private final UUID sessionId;
  private final UUID tenantId;
  private final UUID terminalId;
  private final UUID userId;
  private final String closureType; // e.g., "MANUAL", "AUTO"

  public SessionClosedEvent(
      Object source,
      UUID sessionId,
      UUID tenantId,
      UUID terminalId,
      UUID userId,
      String closureType) {
    super(source);
    this.sessionId = sessionId;
    this.tenantId = tenantId;
    this.terminalId = terminalId;
    this.userId = userId;
    this.closureType = closureType;
  }
}
