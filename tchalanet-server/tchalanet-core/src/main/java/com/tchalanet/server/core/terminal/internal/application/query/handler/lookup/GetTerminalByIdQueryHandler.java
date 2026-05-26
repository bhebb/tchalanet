package com.tchalanet.server.core.terminal.internal.application.query.handler.lookup;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.api.query.GetTerminalByIdQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTerminalByIdQueryHandler
    implements QueryHandler<GetTerminalByIdQuery, TerminalView> {

    private final TerminalReaderPort reader;

    @Override
    public TerminalView handle(GetTerminalByIdQuery query) {
        return TerminalView.from(
            reader.getRequired(query.tenantId(), query.terminalId()));
    }
}
