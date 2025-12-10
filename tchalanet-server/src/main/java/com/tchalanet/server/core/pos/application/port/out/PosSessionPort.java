package com.tchalanet.server.core.pos.application.port.out;

import com.tchalanet.server.core.pos.domain.model.PosSession;
import java.util.Optional;
import java.util.UUID;

public interface PosSessionPort {
  Optional<PosSession> findOpenSessionByTerminal(UUID tenantId, UUID terminalId);
}
