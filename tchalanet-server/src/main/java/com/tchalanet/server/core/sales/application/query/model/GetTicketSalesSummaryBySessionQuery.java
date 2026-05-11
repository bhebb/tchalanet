package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;

public record GetTicketSalesSummaryBySessionQuery(
    TenantId tenantId,
    SalesSessionId sessionId
) implements Query<TicketSalesSessionSummary> {
}
