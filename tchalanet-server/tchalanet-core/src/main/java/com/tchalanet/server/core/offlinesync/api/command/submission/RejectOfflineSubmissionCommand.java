package com.tchalanet.server.core.offlinesync.api.command.submission;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RejectOfflineSubmissionCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSubmissionId submissionId,
    @NotNull UserId decidedBy,
    @NotBlank String reason
) implements Command<Void> {}
