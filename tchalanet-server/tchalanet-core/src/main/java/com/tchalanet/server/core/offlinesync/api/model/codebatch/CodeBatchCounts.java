package com.tchalanet.server.core.offlinesync.api.model.codebatch;

/** Allocation counters of an offline code batch. */
public record CodeBatchCounts(int allocatedCount, int consumedCount) {
    public CodeBatchCounts {
        if (allocatedCount <= 0)
            throw new IllegalArgumentException("allocatedCount must be positive");
        if (consumedCount < 0 || consumedCount > allocatedCount)
            throw new IllegalArgumentException("consumedCount out of range");
    }

    public static CodeBatchCounts initial(int allocatedCount) {
        return new CodeBatchCounts(allocatedCount, 0);
    }

    public CodeBatchCounts incrementConsumed() {
        if (consumedCount + 1 > allocatedCount)
            throw new IllegalStateException("batch allocation exhausted");
        return new CodeBatchCounts(allocatedCount, consumedCount + 1);
    }
}
