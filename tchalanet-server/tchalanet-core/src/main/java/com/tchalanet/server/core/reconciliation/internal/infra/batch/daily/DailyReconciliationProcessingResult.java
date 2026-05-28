package com.tchalanet.server.core.reconciliation.internal.infra.batch.daily;

import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationAnomaly;
import java.util.List;

public record DailyReconciliationProcessingResult(
    DailyReconciliationCounters counters,
    List<ReconciliationAnomaly> anomalies
) {}
