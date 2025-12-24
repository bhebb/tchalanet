package com.tchalanet.server.core.session.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.session.application.query.handler.ListCashierOpenSessionsHandler;
import java.util.List;
import java.util.UUID;

/**
 * Query to list open sessions for a cashier.
 */
public record ListCashierOpenSessionsQuery(
    UUID tenantId,
    UUID userId
) implements Query<List<ListCashierOpenSessionsHandler.CashierSessionDto>> {}
