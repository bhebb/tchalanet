package com.tchalanet.server.core.payout.internal.application.query.handler.reconciliation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.query.reconciliation.ActualTicketStateRow;
import com.tchalanet.server.core.sales.api.query.reconciliation.ListActualTicketStatesForDrawQuery;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

final class PayoutReconciliationRows {

    private PayoutReconciliationRows() {}

    static Map<TicketId, ActualTicketStateRow> ticketRows(QueryBus queryBus, DrawId drawId) {
        return queryBus.ask(new ListActualTicketStatesForDrawQuery(drawId)).stream()
            .collect(Collectors.toMap(ActualTicketStateRow::ticketId, Function.identity()));
    }

    static TicketId ticketId(UUID value) {
        return TicketId.of(value);
    }
}
