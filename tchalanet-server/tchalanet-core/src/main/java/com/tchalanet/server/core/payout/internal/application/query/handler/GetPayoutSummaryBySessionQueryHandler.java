package com.tchalanet.server.core.payout.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutSummaryReaderPort;
import com.tchalanet.server.core.payout.api.query.GetPayoutSummaryBySessionQuery;
import com.tchalanet.server.core.payout.api.query.PayoutSessionSummary;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPayoutSummaryBySessionQueryHandler
    implements QueryHandler<GetPayoutSummaryBySessionQuery, PayoutSessionSummary> {

    private final PayoutSummaryReaderPort reader;

    @Override
    public PayoutSessionSummary handle(GetPayoutSummaryBySessionQuery query) {
        return reader.getByPayingSession(query.tenantId(), query.sessionId());
    }
}
