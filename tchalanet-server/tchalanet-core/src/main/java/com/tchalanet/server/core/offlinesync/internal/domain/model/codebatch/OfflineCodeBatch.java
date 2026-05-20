package com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.api.model.codebatch.CodeBatchCounts;
import com.tchalanet.server.core.offlinesync.api.model.codebatch.CodeBatchLifecycle;
import com.tchalanet.server.core.offlinesync.api.model.codebatch.OfflineCodeBatchStatus;

import java.time.Instant;

public record OfflineCodeBatch(
    CodeBatchIdentity identity,
    CodeBatchCounts counts,
    CodeBatchLifecycle lifecycle
) {

    public OfflineCodeBatch {
        if (identity == null) throw new IllegalArgumentException("identity required");
        if (counts == null) throw new IllegalArgumentException("counts required");
        if (lifecycle == null) throw new IllegalArgumentException("lifecycle required");
    }

    public static OfflineCodeBatch open(
        OfflineCodeBatchId id, TenantId tenantId, OfflineGrantId grantId,
        TerminalId terminalId, OutletId outletId, UserId sellerUserId,
        int allocatedCount, Instant issuedAt, Instant expiresAt
    ) {
        return new OfflineCodeBatch(
            new CodeBatchIdentity(id, tenantId, grantId, terminalId, outletId, sellerUserId),
            CodeBatchCounts.initial(allocatedCount),
            CodeBatchLifecycle.active(issuedAt, expiresAt)
        );
    }

    public OfflineCodeBatch incrementConsumed() {
        if (lifecycle.status() != OfflineCodeBatchStatus.ACTIVE)
            throw new IllegalStateException("batch " + identity.id() + " not ACTIVE");
        return new OfflineCodeBatch(identity, counts.incrementConsumed(), lifecycle);
    }

    public OfflineCodeBatch expire(Instant now) {
        if (lifecycle.status() == OfflineCodeBatchStatus.EXPIRED) return this;
        if (now.isBefore(lifecycle.expiresAt()))
            throw new IllegalStateException("batch " + identity.id() + " not yet expired");
        return new OfflineCodeBatch(identity, counts, lifecycle.expired());
    }

    public boolean isExpired(Instant now) {
        return lifecycle.isExpired(now);
    }

    // Convenience accessors
    public OfflineCodeBatchId id() { return identity.id(); }
    public OfflineCodeBatchStatus status() { return lifecycle.status(); }
}
