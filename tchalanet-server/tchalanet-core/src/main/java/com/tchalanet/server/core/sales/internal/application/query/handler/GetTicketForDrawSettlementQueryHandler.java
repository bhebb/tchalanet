package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.view.TicketForDrawSettlementView;
import com.tchalanet.server.core.sales.api.query.GetTicketForDrawSettlementQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketProjectionReaderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketForDrawSettlementQueryHandler
    implements QueryHandler<GetTicketForDrawSettlementQuery, List<TicketForDrawSettlementView>> {

    private final TicketProjectionReaderPort reader;

    @Override
    public List<TicketForDrawSettlementView> handle(GetTicketForDrawSettlementQuery query) {
        return reader.findForDrawSettlement(query.drawId());
    }
}
