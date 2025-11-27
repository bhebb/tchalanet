package com.tchalanet.server.pos.domain.ports.out;

import com.tchalanet.server.pos.domain.model.PosSession;
import java.util.Optional;
import java.util.UUID;

public interface PosSessionPort {
  Optional<PosSession> findOpenSessionByTerminal(UUID tenantId, UUID terminalId);
}
