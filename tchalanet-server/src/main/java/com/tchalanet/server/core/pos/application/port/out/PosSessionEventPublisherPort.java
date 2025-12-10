package com.tchalanet.server.core.pos.application.port.out;

import java.util.UUID;

public interface PosSessionEventPublisherPort {
  void publishSessionOpenedEvent(UUID sessionId, UUID tenantId, UUID terminalId, UUID userId);

  void publishSessionClosedEvent(
      UUID sessionId, UUID tenantId, UUID terminalId, UUID userId, String closureType);
}
