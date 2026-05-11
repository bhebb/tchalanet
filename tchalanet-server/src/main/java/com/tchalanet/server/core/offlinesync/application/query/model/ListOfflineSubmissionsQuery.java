package com.tchalanet.server.core.offlinesync.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSubmissionStatus;
import java.util.List;

public record ListOfflineSubmissionsQuery(
    TenantId tenantId,
    OfflineBatchId batchId,
    OfflineSubmissionStatus status
) implements Query<List<OfflineSaleSubmission>> {}

