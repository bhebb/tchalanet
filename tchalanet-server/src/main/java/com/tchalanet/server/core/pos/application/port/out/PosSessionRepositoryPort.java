package com.tchalanet.server.core.pos.application.port.out;

import com.tchalanet.server.core.pos.domain.model.PosSession;
import com.tchalanet.server.core.pos.domain.model.PosSessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound Port for persisting and retrieving POS Sessions. */
public interface PosSessionRepositoryPort {
  PosSession save(PosSession session);

  Optional<PosSession> findById(UUID sessionId);

  Optional<PosSession> findByTenantIdAndTerminalIdAndStatus(UUID tenantId, UUID terminalId, PosSessionStatus status);

  Optional<PosSession> findOpenSessionByTerminal(UUID tenantId, UUID terminalId);

  List<PosSession> findOpenSessions(Instant olderThanLastActivity, Instant olderThanOpenedAt);

  // Find sessions for a tenant and user with a given status
  List<PosSession> findByTenantIdAndUserIdAndStatus(UUID tenantId, UUID userId, PosSessionStatus status);
}
