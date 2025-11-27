package com.tchalanet.server.pos.infra.events;

import java.util.UUID;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SessionOpenedEvent extends ApplicationEvent {
  private final UUID sessionId;
  private final UUID tenantId;
  private final UUID terminalId;
  private final UUID userId;

  public SessionOpenedEvent(
      Object source, UUID sessionId, UUID tenantId, UUID terminalId, UUID userId) {
    super(source);
    this.sessionId = sessionId;
    this.tenantId = tenantId;
    this.terminalId = terminalId;
    this.userId = userId;
  }
}
