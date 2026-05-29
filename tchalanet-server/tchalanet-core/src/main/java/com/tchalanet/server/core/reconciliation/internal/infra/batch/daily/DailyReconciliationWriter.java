package com.tchalanet.server.core.reconciliation.internal.infra.batch.daily;

import com.tchalanet.server.core.reconciliation.internal.application.ReconciliationAnomalyWriter;
import java.time.Instant;
import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class DailyReconciliationWriter {

    private final ReconciliationAnomalyWriter anomalyWriter;

    public DailyReconciliationWriter(ReconciliationAnomalyWriter anomalyWriter) {
        this.anomalyWriter = anomalyWriter;
    }

    public DailyReconciliationCounters write(
        Collection<DailyReconciliationProcessingResult> results,
        Instant now
    ) {
        var counters = DailyReconciliationCounters.empty();
        for (var result : results) {
            counters.add(result.counters());
            result.anomalies().forEach(anomaly -> anomalyWriter.upsert(anomaly, now));
        }
        return counters;
    }
}
