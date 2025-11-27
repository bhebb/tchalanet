package com.tchalanet.server.session.domain.ports.in;

import com.tchalanet.server.session.domain.model.PosSession;
import java.math.BigDecimal;
import java.util.UUID;

public interface CloseSessionUseCase {

  record Command(UUID sessionId, BigDecimal closingAmount) {}

  PosSession close(Command command);
}
