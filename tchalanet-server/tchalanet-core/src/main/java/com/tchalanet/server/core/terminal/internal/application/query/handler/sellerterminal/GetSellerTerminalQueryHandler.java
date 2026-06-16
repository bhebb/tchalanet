package com.tchalanet.server.core.terminal.internal.application.query.handler.sellerterminal;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.terminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSellerTerminalQueryHandler
    implements QueryHandler<GetSellerTerminalQuery, SellerTerminalView> {

    private final SellerTerminalReaderPort reader;

    @Override
    public SellerTerminalView handle(GetSellerTerminalQuery q) {
        return SellerTerminalView.from(reader.getRequired(q.tenantId(), q.terminalId()));
    }
}
