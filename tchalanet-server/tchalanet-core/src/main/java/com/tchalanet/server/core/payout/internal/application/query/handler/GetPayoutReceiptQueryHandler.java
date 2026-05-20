package com.tchalanet.server.core.payout.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutQueryReaderPort;
import com.tchalanet.server.core.payout.api.query.GetPayoutReceiptQuery;
import com.tchalanet.server.core.payout.api.query.PayoutReceiptView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPayoutReceiptQueryHandler
    implements QueryHandler<GetPayoutReceiptQuery, PayoutReceiptView> {

    private final PayoutQueryReaderPort reader;

    @Override
    public PayoutReceiptView handle(GetPayoutReceiptQuery query) {
        return reader.getReceiptViewById(query.payoutId());
    }
}
