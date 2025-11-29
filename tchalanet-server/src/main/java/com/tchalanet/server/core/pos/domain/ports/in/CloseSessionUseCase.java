package com.tchalanet.server.core.pos.domain.ports.in;

import com.tchalanet.server.core.pos.domain.model.PosSession;
import java.math.BigDecimal;
import java.util.UUID;

public interface CloseSessionUseCase {
  PosSession closeSession(CloseSessionCommand command);

  record CloseSessionCommand(
      UUID tenantId,
      UUID sessionId,
      UUID userId, // User performing the close
      BigDecimal closingAmount // Optional
      ) {}
}
