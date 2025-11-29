package com.tchalanet.server.core.session.domain.ports.out;

import com.tchalanet.server.core.session.domain.model.PosSession;
import java.util.Optional;
import java.util.UUID;

public interface SessionReadPort {

  Optional<PosSession> findOpenForTerminal(UUID tenantId, UUID terminalId);
}
