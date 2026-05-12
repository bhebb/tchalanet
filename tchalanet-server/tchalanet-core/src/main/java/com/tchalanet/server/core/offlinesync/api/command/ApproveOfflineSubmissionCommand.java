package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.UserId;

public record ApproveOfflineSubmissionCommand(
    OfflineSaleSubmissionId submissionId,
    UserId performedBy,
    String reason
) implements Command<ApproveOfflineSubmissionResult> {}

