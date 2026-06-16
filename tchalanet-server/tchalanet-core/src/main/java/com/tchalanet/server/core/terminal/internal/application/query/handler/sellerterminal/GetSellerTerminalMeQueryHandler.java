package com.tchalanet.server.core.terminal.internal.application.query.handler.sellerterminal;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.terminal.api.query.GetSellerTerminalMeQuery;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSellerTerminalMeQueryHandler
    implements QueryHandler<GetSellerTerminalMeQuery, SellerTerminalView> {

    private final SellerTerminalReaderPort reader;

    @Override
    public SellerTerminalView handle(GetSellerTerminalMeQuery q) {
        return SellerTerminalView.from(reader.getRequired(q.tenantId(), q.sellerTerminalId()));
    }
}
