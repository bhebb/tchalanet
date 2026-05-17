package com.tchalanet.server.core.offlinesync.internal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record AdminRejectOfflineSubmissionCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSaleSubmissionId submissionId,
    @NotNull UserId rejectedBy,
    @NotNull String reason
) implements Command<Void> {}
