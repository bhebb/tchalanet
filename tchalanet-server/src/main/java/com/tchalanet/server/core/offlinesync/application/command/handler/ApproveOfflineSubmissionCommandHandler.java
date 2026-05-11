package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.ApproveOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.ApproveOfflineSubmissionResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineSubmissionReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ApproveOfflineSubmissionCommandHandler
    implements CommandHandler<ApproveOfflineSubmissionCommand, ApproveOfflineSubmissionResult> {

  private final OfflineSubmissionReaderPort reader;

  @Override
  @TchTx
  public ApproveOfflineSubmissionResult handle(ApproveOfflineSubmissionCommand cmd) {
    var submission = reader.findById(cmd.submissionId())
        .orElseThrow(() -> new IllegalArgumentException("Offline submission not found: " + cmd.submissionId()));

    // TODO:
    // Approve review decision.
    // Approve should call Sales to create the ticket.
    // Reject should only mark offlinesync decision and never create a Sales ticket.

    return new ApproveOfflineSubmissionResult(cmd.submissionId(), null);
  }
}
