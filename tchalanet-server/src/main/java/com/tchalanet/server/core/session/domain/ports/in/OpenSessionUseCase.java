package com.tchalanet.server.core.session.domain.ports.in;

import com.tchalanet.server.core.session.domain.model.PosSession;
import java.math.BigDecimal;
import java.util.UUID;

public interface OpenSessionUseCase {

  record Command(
      UUID tenantId, UUID outletId, UUID terminalId, UUID userId, BigDecimal openingFloat) {}

  PosSession open(Command command);
}
