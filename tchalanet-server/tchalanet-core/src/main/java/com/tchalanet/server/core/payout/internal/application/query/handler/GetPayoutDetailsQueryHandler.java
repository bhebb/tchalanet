package com.tchalanet.server.core.payout.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutQueryReaderPort;
import com.tchalanet.server.core.payout.api.query.GetPayoutDetailsQuery;
import com.tchalanet.server.core.payout.api.query.PayoutDetails;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPayoutDetailsQueryHandler
    implements QueryHandler<GetPayoutDetailsQuery, PayoutDetails> {

    private final PayoutQueryReaderPort reader;

    @Override
    public PayoutDetails handle(GetPayoutDetailsQuery query) {
        return reader.getDetailsById(query.payoutId());
    }
}
