package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.RejectOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.RejectOfflineSubmissionResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineSubmissionReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RejectOfflineSubmissionCommandHandler
    implements CommandHandler<RejectOfflineSubmissionCommand, RejectOfflineSubmissionResult> {

  private final OfflineSubmissionReaderPort reader;

  @Override
  @TchTx
  public RejectOfflineSubmissionResult handle(RejectOfflineSubmissionCommand cmd) {
    var submission = reader.findById(cmd.submissionId())
        .orElseThrow(() -> new IllegalArgumentException("Offline submission not found: " + cmd.submissionId()));

    // TODO:
    // Reject review decision.
    // Approve should call Sales to create the ticket.
    // Reject should only mark offlinesync decision and never create a Sales ticket.

    return new RejectOfflineSubmissionResult(cmd.submissionId(), null);
  }
}
