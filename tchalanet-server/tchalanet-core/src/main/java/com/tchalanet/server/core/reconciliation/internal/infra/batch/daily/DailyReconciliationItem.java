package com.tchalanet.server.core.reconciliation.internal.infra.batch.daily;

import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.drawresult.api.query.reconciliation.ReconciliationDrawResultRow;
import java.time.LocalDate;

public record DailyReconciliationItem(
    ReconciliationRunId runId,
    TenantId tenantId,
    LocalDate businessDate,
    ReconciliationDrawResultRow drawResult
) {}
