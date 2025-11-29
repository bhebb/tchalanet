package com.tchalanet.server.core.pos.domain.ports.in;

import com.tchalanet.server.core.pos.domain.model.PosSession;
import java.math.BigDecimal;
import java.util.UUID;

public interface OpenSessionUseCase {
  PosSession openSession(OpenSessionCommand command);

  record OpenSessionCommand(
      UUID tenantId, UUID terminalId, UUID userId, BigDecimal openingFloat // Optional
      ) {}
}
