package com.tchalanet.server.core.session.domain.ports.out;

import com.tchalanet.server.core.session.domain.model.PosSession;
import java.util.Optional;
import java.util.UUID;

public interface PosSessionRepositoryPort {

  PosSession save(PosSession session);

  Optional<PosSession> findById(UUID id);

  Optional<PosSession> findOpenByTerminal(UUID tenantId, UUID terminalId);
}
