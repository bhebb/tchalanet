package com.tchalanet.server.core.offlinesync.application.command.handler.review;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.core.offlinesync.application.command.model.review.RejectOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.review.RejectOfflineSubmissionResult;

public class RejectOfflineSubmissionCommandHandler
    implements CommandHandler<RejectOfflineSubmissionCommand, RejectOfflineSubmissionResult> {

  @Override
  public RejectOfflineSubmissionResult handle(RejectOfflineSubmissionCommand command) {
    return new RejectOfflineSubmissionResult(command.submissionId(), "SALES_REJECTED");
  }
}

