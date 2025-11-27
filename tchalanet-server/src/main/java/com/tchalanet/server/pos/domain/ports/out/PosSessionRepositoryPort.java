package com.tchalanet.server.pos.domain.ports.out;

import com.tchalanet.server.pos.domain.model.PosSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound Port for persisting and retrieving POS Sessions. */
public interface PosSessionRepositoryPort {
  PosSession save(PosSession session);

  Optional<PosSession> findById(UUID sessionId);

  Optional<PosSession> findOpenSessionByTerminal(UUID tenantId, UUID terminalId);

  List<PosSession> findOpenSessions(Instant olderThanLastActivity, Instant olderThanOpenedAt);
  // Add methods for aggregation if needed
}
