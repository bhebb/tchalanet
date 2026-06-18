package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListSellerTerminalsQueryHandler
    implements QueryHandler<ListSellerTerminalsQuery, TchPage<SellerTerminalSummaryRow>> {

    private final SellerTerminalReaderPort reader;

    @Override
    public TchPage<SellerTerminalSummaryRow> handle(ListSellerTerminalsQuery q) {
        return reader.search(q.tenantId(), q.criteria(), q.pageRequest());
    }
}
