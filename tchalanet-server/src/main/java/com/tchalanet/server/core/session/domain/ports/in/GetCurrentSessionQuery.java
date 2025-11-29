package com.tchalanet.server.core.session.domain.ports.in;

import com.tchalanet.server.core.session.domain.model.PosSession;
import java.util.Optional;
import java.util.UUID;

public interface GetCurrentSessionQuery {

  Optional<PosSession> get(UUID tenantId, UUID terminalId);
}
