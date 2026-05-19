package com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.TenantId;

/** Stable identity of a sync batch, including its idempotence key {@code clientBatchId}. */
public record SyncBatchIdentity(
    OfflineSyncBatchId id,
    TenantId tenantId,
    OfflineGrantId grantId,
    OfflineCodeBatchId codeBatchId,
    String clientBatchId
) {
    public SyncBatchIdentity {
        if (id == null) throw new IllegalArgumentException("id required");
        if (tenantId == null) throw new IllegalArgumentException("tenantId required");
        if (grantId == null) throw new IllegalArgumentException("grantId required");
        if (clientBatchId == null || clientBatchId.isBlank())
            throw new IllegalArgumentException("clientBatchId required");
    }
}
