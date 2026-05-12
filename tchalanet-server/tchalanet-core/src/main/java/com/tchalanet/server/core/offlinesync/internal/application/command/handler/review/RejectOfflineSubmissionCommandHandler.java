package com.tchalanet.server.core.offlinesync.internal.application.command.handler.review;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.core.offlinesync.api.command.RejectOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.command.RejectOfflineSubmissionResult;

public class RejectOfflineSubmissionCommandHandler
    implements CommandHandler<RejectOfflineSubmissionCommand, RejectOfflineSubmissionResult> {

  @Override
  public RejectOfflineSubmissionResult handle(RejectOfflineSubmissionCommand command) {
    return new RejectOfflineSubmissionResult(command.submissionId(), "SALES_REJECTED");
  }
}

