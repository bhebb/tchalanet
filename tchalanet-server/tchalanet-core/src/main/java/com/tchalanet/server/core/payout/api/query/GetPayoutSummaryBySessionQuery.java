package com.tchalanet.server.core.payout.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;

public record GetPayoutSummaryBySessionQuery(
    TenantId tenantId,
    SalesSessionId sessionId
) implements Query<PayoutSessionSummary> {}
