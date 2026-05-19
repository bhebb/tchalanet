package com.tchalanet.server.core.offlinesync.api.query.submission;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;

import java.util.List;
import java.util.Set;

public record ListOfflineSubmissionsQuery(
    TenantId tenantId,
    UserId sellerUserId,
    Set<OfflineSubmissionStatus> statuses,
    int limit
) implements Query<List<OfflineSubmissionView>> {}
