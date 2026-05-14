package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.api.model.TicketSaleStatus;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.query.TicketSalesSessionSummary;

import java.util.Set;

public interface TicketSalesSummaryReaderPort {
    TicketSalesSessionSummary getBySession(TenantId tenantId, SalesSessionId sessionId);
}
