package com.tchalanet.server.core.payout.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.payout.application.port.out.PayoutQueryReaderPort;
import com.tchalanet.server.core.payout.application.query.model.ListPayoutsQuery;
import com.tchalanet.server.core.payout.application.query.model.PayoutRow;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPayoutsQueryHandler
    implements QueryHandler<ListPayoutsQuery, TchPage<PayoutRow>> {

    private final PayoutQueryReaderPort reader;

    @Override
    public TchPage<PayoutRow> handle(ListPayoutsQuery query) {
        return reader.list(query);
    }
}
