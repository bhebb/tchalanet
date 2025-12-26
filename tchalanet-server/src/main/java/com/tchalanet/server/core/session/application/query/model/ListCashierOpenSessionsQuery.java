package com.tchalanet.server.core.session.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.session.application.query.handler.ListCashierOpenSessionsHandler;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.List;

/**
 * Query to list open sessions for a cashier.
 */
public record ListCashierOpenSessionsQuery(
    TenantId tenantId,
    UserId userId
) implements Query<List<ListCashierOpenSessionsHandler.CashierSessionDto>> {}
