package com.tchalanet.server.core.offlinesync.api.model.syncbatch;

/** Per-submission counters of a sync batch after processing. */
public record SyncBatchCounters(
    int submissionCount,
    int technicalRejectCount,
    int salesAcceptCount,
    int salesRejectCount,
    int reviewCount
) {
    public SyncBatchCounters {
        if (submissionCount < 0)
            throw new IllegalArgumentException("submissionCount cannot be negative");
        if (technicalRejectCount < 0 || salesAcceptCount < 0 || salesRejectCount < 0 || reviewCount < 0)
            throw new IllegalArgumentException("counters cannot be negative");
    }

    public static SyncBatchCounters initial(int submissionCount) {
        return new SyncBatchCounters(submissionCount, 0, 0, 0, 0);
    }

    public int totalProcessed() {
        return technicalRejectCount + salesAcceptCount + salesRejectCount;
    }
}
