package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.domain.model.SalesSession;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port for reading POS sessions.
 */
public interface SalesSessionReaderPort {

    Optional<SalesSession> findById(TenantId tenantId, SalesSessionId id);

    SalesSession getById(TenantId tenantId, SalesSessionId id);

    Optional<SalesSession> findOpenByTerminal(TenantId tenantId, TerminalId terminalId);

    Optional<SalesSession> findCurrentOpenByUser(TenantId tenantId, UserId userId);

    boolean hasOpenSessions(OutletId outletId);

    List<SalesSessionId> findSessionIds(OutletId outletId, Instant from, Instant to);

    boolean existsForBusinessDate(TenantId tenantId, OutletId id, UserId openedBy, LocalDate businessDate);

    List<SalesSession> findOpenedSalesSession(TenantId tenantId, TerminalId terminalId, OutletId outletId, UserId userId);

}
