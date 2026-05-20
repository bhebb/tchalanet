package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch.OfflineSyncBatch;

import java.util.Optional;

public interface OfflineSyncBatchReaderPort {

    Optional<OfflineSyncBatch> findById(OfflineSyncBatchId id);

    /** {@link #findById(OfflineSyncBatchId)} variant that 404s when missing. */
    OfflineSyncBatch getRequired(OfflineSyncBatchId id);

    /** Idempotence lookup by {@code clientBatchId} for a given grant. */
    Optional<OfflineSyncBatch> findByClientBatchId(
        TenantId tenantId, OfflineGrantId grantId, String clientBatchId);
}
