package com.tchalanet.server.core.offlinesync.api.model.syncbatch;

import java.time.Instant;

/** Lifecycle trace of an offline sync batch. */
public record SyncBatchLifecycle(
    OfflineSyncBatchStatus status,
    Instant receivedAt,
    Instant processedAt
) {
    public SyncBatchLifecycle {
        if (status == null) throw new IllegalArgumentException("status required");
        if (receivedAt == null) throw new IllegalArgumentException("receivedAt required");
    }

    public static SyncBatchLifecycle received(Instant receivedAt) {
        return new SyncBatchLifecycle(OfflineSyncBatchStatus.RECEIVED, receivedAt, null);
    }

    public SyncBatchLifecycle transitionTo(OfflineSyncBatchStatus next, Instant processedAt) {
        if (next == null) throw new IllegalArgumentException("next status required");
        return new SyncBatchLifecycle(next, receivedAt, processedAt);
    }
}
