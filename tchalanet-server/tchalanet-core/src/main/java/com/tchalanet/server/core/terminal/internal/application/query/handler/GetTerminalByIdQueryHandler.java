package com.tchalanet.server.core.terminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.query.model.GetTerminalByIdQuery;
import com.tchalanet.server.core.terminal.application.query.model.TerminalView;
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
