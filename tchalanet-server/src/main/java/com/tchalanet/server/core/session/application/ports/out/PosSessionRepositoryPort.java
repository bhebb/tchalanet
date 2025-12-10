package com.tchalanet.server.core.session.application.ports.out;

import com.tchalanet.server.core.session.domain.model.PosSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosSessionRepositoryPort {

  PosSession save(PosSession session);

  Optional<PosSession> findById(UUID id);

  Optional<PosSession> findOpenByTerminal(UUID tenantId, UUID terminalId);

  List<PosSession> findByTenantIdAndUserIdAndStatus(UUID tenantId, UUID userId, com.tchalanet.server.core.session.domain.model.PosSessionStatus status);
}
