package com.tchalanet.server.core.offlinesync.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Port pour suivre l'état de synchronisation par terminal / device. */
public interface SyncStatePort {

  Optional<Instant> getLastSync(UUID tenantId, UUID terminalId);

  void updateLastSync(UUID tenantId, UUID terminalId, Instant at);
}
