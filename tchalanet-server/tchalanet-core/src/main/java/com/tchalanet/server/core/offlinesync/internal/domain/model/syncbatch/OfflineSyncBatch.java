package com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.api.model.syncbatch.OfflineSyncBatchStatus;
import com.tchalanet.server.core.offlinesync.api.model.syncbatch.SyncBatchCounters;
import com.tchalanet.server.core.offlinesync.api.model.syncbatch.SyncBatchLifecycle;

import java.time.Instant;
import java.util.UUID;

public record OfflineSyncBatch(
    SyncBatchIdentity identity,
    SyncBatchContext context,
    SyncBatchCounters counters,
    SyncBatchLifecycle lifecycle
) {

    public OfflineSyncBatch {
        if (identity == null) throw new IllegalArgumentException("identity required");
        if (context == null) throw new IllegalArgumentException("context required");
        if (counters == null) throw new IllegalArgumentException("counters required");
        if (lifecycle == null) throw new IllegalArgumentException("lifecycle required");
    }

    public static OfflineSyncBatch open(
        OfflineSyncBatchId id, TenantId tenantId, OfflineGrantId grantId,
        OfflineCodeBatchId codeBatchId, UserId sellerUserId,
        TerminalId terminalId, OutletId outletId, SalesSessionId salesSessionId,
        UUID deviceId, String clientBatchId, int submissionCount, Instant receivedAt
    ) {
        return new OfflineSyncBatch(
            new SyncBatchIdentity(id, tenantId, grantId, codeBatchId, clientBatchId),
            new SyncBatchContext(deviceId, sellerUserId, terminalId, outletId, salesSessionId),
            SyncBatchCounters.initial(submissionCount),
            SyncBatchLifecycle.received(receivedAt)
        );
    }

    public OfflineSyncBatch withCounters(
        int techReject, int salesAccept, int salesReject, int review, Instant processedAt
    ) {
        SyncBatchCounters newCounters = new SyncBatchCounters(
            counters.submissionCount(), techReject, salesAccept, salesReject, review);
        OfflineSyncBatchStatus next = computeStatus(newCounters);
        return new OfflineSyncBatch(
            identity, context, newCounters,
            lifecycle.transitionTo(next, processedAt)
        );
    }

    private static OfflineSyncBatchStatus computeStatus(SyncBatchCounters c) {
        if (c.submissionCount() == 0) return OfflineSyncBatchStatus.COMPLETED;
        int processed = c.totalProcessed();
        if (processed == 0 && c.reviewCount() == 0) return OfflineSyncBatchStatus.RECEIVED;
        if (c.reviewCount() > 0) return OfflineSyncBatchStatus.PARTIALLY_ACCEPTED;
        if (c.technicalRejectCount() + c.salesRejectCount() == c.submissionCount())
            return OfflineSyncBatchStatus.REJECTED;
        if (c.salesAcceptCount() == c.submissionCount())
            return OfflineSyncBatchStatus.ACCEPTED;
        return OfflineSyncBatchStatus.PARTIALLY_ACCEPTED;
    }

    // Convenience accessors
    public OfflineSyncBatchId id() { return identity.id(); }
    public OfflineSyncBatchStatus status() { return lifecycle.status(); }
}
