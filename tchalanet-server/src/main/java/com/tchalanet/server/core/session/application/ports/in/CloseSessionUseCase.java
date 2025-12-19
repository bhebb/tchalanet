package com.tchalanet.server.core.session.application.ports.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface CloseSessionUseCase {

  record Command(UUID sessionId, BigDecimal closingAmount) {}

  PosSession close(Command command);
}
