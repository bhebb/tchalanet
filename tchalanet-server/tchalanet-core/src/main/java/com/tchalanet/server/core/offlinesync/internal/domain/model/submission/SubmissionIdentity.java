package com.tchalanet.server.core.offlinesync.internal.domain.model.submission;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.TenantId;

/** Stable identity of a submission, including idempotence key {@code clientSubmissionId}. */
public record SubmissionIdentity(
    OfflineSubmissionId id,
    TenantId tenantId,
    OfflineSyncBatchId syncBatchId,
    OfflineGrantId grantId,
    OfflineCodeBatchId codeBatchId,
    String offlineCode,
    String clientSubmissionId
) {
    public SubmissionIdentity {
        if (id == null) throw new IllegalArgumentException("id required");
        if (tenantId == null) throw new IllegalArgumentException("tenantId required");
        if (grantId == null) throw new IllegalArgumentException("grantId required");
        if (clientSubmissionId == null || clientSubmissionId.isBlank())
            throw new IllegalArgumentException("clientSubmissionId required");
    }
}
