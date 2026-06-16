package com.tchalanet.server.core.terminal.internal.application.query.handler.sellerterminal;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.terminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalReaderPort;
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
