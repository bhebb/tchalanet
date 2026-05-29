package com.tchalanet.server.core.reconciliation.internal.infra.batch.daily;

import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationSeverity;

public final class DailyReconciliationCounters {

    private long checkedDrawCount;
    private long checkedTicketCount;
    private long anomalyCount;
    private long criticalCount;
    private long highCount;
    private long mediumCount;
    private long lowCount;

    public static DailyReconciliationCounters empty() {
        return new DailyReconciliationCounters();
    }

    public static DailyReconciliationCounters forSeverity(ReconciliationSeverity severity) {
        var counters = new DailyReconciliationCounters();
        counters.addSeverity(severity);
        return counters;
    }

    public void add(DailyReconciliationCounters other) {
        this.checkedDrawCount += other.checkedDrawCount;
        this.checkedTicketCount += other.checkedTicketCount;
        this.anomalyCount += other.anomalyCount;
        this.criticalCount += other.criticalCount;
        this.highCount += other.highCount;
        this.mediumCount += other.mediumCount;
        this.lowCount += other.lowCount;
    }

    public void incrementCheckedDrawCount() {
        checkedDrawCount++;
    }

    public void addCheckedTicketCount(long count) {
        checkedTicketCount += count;
    }

    private void addSeverity(ReconciliationSeverity severity) {
        anomalyCount++;
        switch (severity) {
            case CRITICAL -> criticalCount++;
            case HIGH -> highCount++;
            case MEDIUM -> mediumCount++;
            case LOW -> lowCount++;
        }
    }

    public long checkedDrawCount() {
        return checkedDrawCount;
    }

    public long checkedTicketCount() {
        return checkedTicketCount;
    }

    public long anomalyCount() {
        return anomalyCount;
    }

    public long criticalCount() {
        return criticalCount;
    }

    public long highCount() {
        return highCount;
    }

    public long mediumCount() {
        return mediumCount;
    }

    public long lowCount() {
        return lowCount;
    }
}
