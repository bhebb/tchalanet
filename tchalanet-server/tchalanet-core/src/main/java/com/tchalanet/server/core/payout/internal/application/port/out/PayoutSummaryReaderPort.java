package com.tchalanet.server.core.payout.internal.application.port.out;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.payout.api.query.PayoutSessionSummary;

public interface PayoutSummaryReaderPort {
    PayoutSessionSummary getByPayingSession(TenantId tenantId, SalesSessionId sessionId);
}
