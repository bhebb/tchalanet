package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.core.session.domain.model.PosSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for reading POS sessions.
 */
public interface PosSessionReaderPort {

  Optional<PosSession> findById(UUID id);

  Optional<PosSession> findOpenByTerminal(UUID tenantId, UUID terminalId);

  List<PosSession> findOpenByCashier(UUID tenantId, UUID userId);
}
