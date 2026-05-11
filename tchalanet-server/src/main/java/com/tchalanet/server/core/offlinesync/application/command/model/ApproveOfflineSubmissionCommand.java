package com.tchalanet.server.core.offlinesync.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApproveOfflineSubmissionCommand(
    @NotNull OfflineSaleSubmissionId submissionId,
    @NotNull UserId performedBy,
    @NotBlank String reason
) implements Command<ApproveOfflineSubmissionResult> {}
