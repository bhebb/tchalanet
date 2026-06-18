package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.core.sellerterminal.internal.application.mapper.SellerTerminalViews;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSellerTerminalQueryHandler
    implements QueryHandler<GetSellerTerminalQuery, SellerTerminalView> {

    private final SellerTerminalReaderPort reader;

    @Override
    public SellerTerminalView handle(GetSellerTerminalQuery q) {
        return SellerTerminalViews.detail(reader.getRequired(q.tenantId(), q.terminalId()));
    }
}
