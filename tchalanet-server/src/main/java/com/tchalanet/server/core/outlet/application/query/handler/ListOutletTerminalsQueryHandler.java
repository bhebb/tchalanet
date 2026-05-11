package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletTerminalReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletTerminalsQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletTerminalView;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListOutletTerminalsQueryHandler
    implements QueryHandler<ListOutletTerminalsQuery, List<OutletTerminalView>> {

    private final OutletReaderPort outletReader;
    private final OutletTerminalReaderPort terminalReader;

    @Override
    public List<OutletTerminalView> handle(ListOutletTerminalsQuery query) {
        outletReader.getRequired(query.outletId());
        return terminalReader.listTerminalsByOutlet(query.outletId());
    }
}
