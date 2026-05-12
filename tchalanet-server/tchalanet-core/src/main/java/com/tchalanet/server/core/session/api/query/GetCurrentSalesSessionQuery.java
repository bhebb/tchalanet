package com.tchalanet.server.core.session.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import java.util.Optional;

/** Query to get the current open session for a terminal. */
public record GetCurrentSalesSessionQuery(TenantId tenantId, TerminalId terminalId)
    implements Query<Optional<SalesSession>> {}
