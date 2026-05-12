package com.tchalanet.server.core.offlinesync.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmissionStatus;
import java.util.List;

public record ListOfflineSubmissionsQuery(
    TenantId tenantId,
    OfflineBatchId batchId,
    OfflineSubmissionStatus status
) implements Query<List<OfflineSaleSubmission>> {}

