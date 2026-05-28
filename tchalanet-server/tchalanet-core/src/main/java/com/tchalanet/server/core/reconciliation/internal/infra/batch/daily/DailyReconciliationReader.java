package com.tchalanet.server.core.reconciliation.internal.infra.batch.daily;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.drawresult.api.query.reconciliation.ListReconciliationDrawResultsQuery;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DailyReconciliationReader {

    private final QueryBus queryBus;

    public DailyReconciliationReader(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    public List<DailyReconciliationItem> read(
        ReconciliationRunId runId,
        TenantId tenantId,
        LocalDate businessDate
    ) {
        return queryBus.ask(new ListReconciliationDrawResultsQuery(tenantId, businessDate))
            .stream()
            .map(drawResult -> new DailyReconciliationItem(runId, tenantId, businessDate, drawResult))
            .toList();
    }
}
