package com.tchalanet.server.core.offlinesync.internal.application.command.handler.submission;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.command.submission.RejectOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.service.decision.OfflineSubmissionDecisionRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RejectOfflineSubmissionCommandHandler
    implements CommandHandler<RejectOfflineSubmissionCommand, Void> {

    private final OfflineSubmissionReaderPort submissionReader;
    private final OfflineSubmissionWriterPort submissionWriter;
    private final OfflineSubmissionDecisionRecorder decisionRecorder;
    private final Clock clock;

    @Override
    @TchTx
    public Void handle(RejectOfflineSubmissionCommand command) {
        var submission = submissionReader.getRequired(command.submissionId());
        var now = clock.instant();
        submissionWriter.save(submission.markAdminRejected(command.reason(), now));
        decisionRecorder.record(
            command.tenantId(), command.submissionId(), command.decidedBy(),
            OfflineSubmissionDecisionRecorder.DecisionType.REJECT,
            command.reason(), now, false, null);
        log.info("offlinesync: submission {} admin-rejected by {} reason='{}'",
            command.submissionId(), command.decidedBy(), command.reason());
        return null;
    }
}
