package com.tchalanet.server.core.session.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;

import java.util.List;

/** Query to get the current open session for a terminal. */
public record GetOpenedSalesSessionQuery(TenantId tenantId, OutletId outletId, UserId userId, TerminalId terminalId)
    implements Query<List<SalesSession>> {}
