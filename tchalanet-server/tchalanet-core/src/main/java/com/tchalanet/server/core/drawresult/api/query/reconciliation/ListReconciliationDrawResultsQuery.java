package com.tchalanet.server.core.drawresult.api.query.reconciliation;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.List;

public record ListReconciliationDrawResultsQuery(
    TenantId tenantId,
    LocalDate businessDate
) implements Query<List<ReconciliationDrawResultRow>> {}
