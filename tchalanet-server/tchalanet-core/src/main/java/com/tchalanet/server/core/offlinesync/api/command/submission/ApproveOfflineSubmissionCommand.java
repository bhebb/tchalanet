package com.tchalanet.server.core.offlinesync.api.command.submission;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

/** Admin force-approves a submission previously in BUSINESS_REJECTED or NEEDS_ADMIN_REVIEW. */
public record ApproveOfflineSubmissionCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSubmissionId submissionId,
    @NotNull UserId decidedBy,
    String reason
) implements Command<Void> {}
