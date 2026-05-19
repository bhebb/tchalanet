package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.OfflineSubmission;

import java.util.List;
import java.util.Optional;

public interface OfflineSubmissionReaderPort {

    Optional<OfflineSubmission> findById(OfflineSubmissionId id);

    /** {@link #findById(OfflineSubmissionId)} variant that 404s when missing. */
    OfflineSubmission getRequired(OfflineSubmissionId id);

    /** Idempotence lookup by {@code clientSubmissionId} within a grant scope. */
    Optional<OfflineSubmission> findByClientSubmissionId(
        TenantId tenantId, OfflineGrantId grantId, String clientSubmissionId);

    List<OfflineSubmission> listForSeller(TenantId tenantId, UserId sellerUserId, int limit);
}
