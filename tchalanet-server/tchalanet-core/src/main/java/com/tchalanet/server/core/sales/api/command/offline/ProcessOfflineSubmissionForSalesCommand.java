package com.tchalanet.server.core.sales.api.command.offline;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

public record ProcessOfflineSubmissionForSalesCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSaleSubmissionId submissionId
) implements Command<ProcessOfflineSubmissionForSalesResult> {}
