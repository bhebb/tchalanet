package com.tchalanet.server.core.offlinesync.api.query.submission;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;

public record GetOfflineSubmissionQuery(TenantId tenantId, OfflineSubmissionId submissionId)
    implements Query<OfflineSubmissionView> {
}
