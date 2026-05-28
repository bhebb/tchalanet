package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.query.ListDrawReconciliationRowsQuery;
import com.tchalanet.server.core.drawresult.api.query.reconciliation.ListReconciliationDrawResultsQuery;
import com.tchalanet.server.core.drawresult.api.query.reconciliation.ReconciliationDrawResultRow;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListReconciliationDrawResultsQueryHandler
    implements QueryHandler<ListReconciliationDrawResultsQuery, List<ReconciliationDrawResultRow>> {

    private final QueryBus queryBus;
    private final DrawResultReaderPort drawResultReader;

    @Override
    public List<ReconciliationDrawResultRow> handle(ListReconciliationDrawResultsQuery query) {
        return queryBus.ask(new ListDrawReconciliationRowsQuery(query.tenantId(), query.businessDate())).stream()
            .filter(draw -> draw.drawResultId() != null)
            .map(draw -> {
                var result = drawResultReader.findViewById(draw.drawResultId()).orElse(null);
                return new ReconciliationDrawResultRow(
                    draw.drawId(),
                    draw.drawResultId(),
                    draw.drawChannelId(),
                    draw.drawDate(),
                    draw.scheduledAt(),
                    draw.resultedAt(),
                    draw.status(),
                    result == null ? null : result.status()
                );
            })
            .toList();
    }
}
