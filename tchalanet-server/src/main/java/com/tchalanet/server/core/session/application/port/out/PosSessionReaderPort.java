package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.domain.model.PosSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for reading POS sessions. */
public interface PosSessionReaderPort {

  Optional<PosSession> findById(UUID id);

  Optional<PosSession> findOpenByTerminal(TenantId tenantId, TerminalId terminalId);

  List<PosSession> findOpenByCashier(TenantId tenantId, UserId userId);
}
