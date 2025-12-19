package com.tchalanet.server.core.session.application.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface SessionReadPort {

  Optional<PosSession> findOpenForTerminal(UUID tenantId, UUID terminalId);
}
