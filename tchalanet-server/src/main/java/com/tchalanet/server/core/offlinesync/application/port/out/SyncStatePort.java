package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;
import java.util.Optional;

/** Port pour suivre l'état de synchronisation par terminal / device. */
public interface SyncStatePort {

  Optional<Instant> getLastSync(TenantId tenantId, TerminalId terminalId);

  void updateLastSync(TenantId tenantId, TerminalId terminalId, Instant at);
}
