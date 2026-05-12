package com.tchalanet.server.core.offlinesync.internal.application.command.handler.review;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.core.offlinesync.api.command.ApproveOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.command.ApproveOfflineSubmissionResult;

public class ApproveOfflineSubmissionCommandHandler
    implements CommandHandler<ApproveOfflineSubmissionCommand, ApproveOfflineSubmissionResult> {

  @Override
  public ApproveOfflineSubmissionResult handle(ApproveOfflineSubmissionCommand command) {
    return new ApproveOfflineSubmissionResult(command.submissionId(), null);
  }
}

