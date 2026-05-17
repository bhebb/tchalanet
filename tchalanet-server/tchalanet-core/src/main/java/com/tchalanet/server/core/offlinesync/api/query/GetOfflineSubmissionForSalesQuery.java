package com.tchalanet.server.core.offlinesync.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionForSalesView;
import jakarta.validation.constraints.NotNull;

public record GetOfflineSubmissionForSalesQuery(
    @NotNull TenantId tenantId,
    @NotNull OfflineSaleSubmissionId submissionId
) implements Query<OfflineSubmissionForSalesView> {}
