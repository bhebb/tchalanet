package com.tchalanet.server.pos.infra.events;

import com.tchalanet.server.pos.domain.ports.out.PosSessionEventPublisherPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringPosSessionEventPublisherAdapter implements PosSessionEventPublisherPort {

  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void publishSessionOpenedEvent(
      UUID sessionId, UUID tenantId, UUID terminalId, UUID userId) {
    log.info("Publishing SessionOpenedEvent for session: {}", sessionId);
    SessionOpenedEvent event =
        new SessionOpenedEvent(this, sessionId, tenantId, terminalId, userId);
    eventPublisher.publishEvent(event);
  }

  @Override
  public void publishSessionClosedEvent(
      UUID sessionId, UUID tenantId, UUID terminalId, UUID userId, String closureType) {
    log.info("Publishing SessionClosedEvent for session: {} (Type: {})", sessionId, closureType);
    SessionClosedEvent event =
        new SessionClosedEvent(this, sessionId, tenantId, terminalId, userId, closureType);
    eventPublisher.publishEvent(event);
  }
}
