package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.domain.model.SalesSession;

import java.util.List;
import java.util.Optional;

/**
 * Port for reading POS sessions.
 */
public interface SalesSessionReaderPort {

    Optional<SalesSession> findById(SessionId id);

    Optional<SalesSession> findOpenByTerminal(TenantId tenantId, TerminalId terminalId);

    List<SalesSession> findOpenByCashier(TenantId tenantId, UserId userId);
}
