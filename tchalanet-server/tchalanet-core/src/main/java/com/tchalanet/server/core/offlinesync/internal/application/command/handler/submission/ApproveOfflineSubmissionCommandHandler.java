package com.tchalanet.server.core.offlinesync.internal.application.command.handler.submission;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.command.submission.ApproveOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.service.decision.OfflineSubmissionDecisionRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ApproveOfflineSubmissionCommandHandler
    implements CommandHandler<ApproveOfflineSubmissionCommand, Void> {

    private final OfflineSubmissionReaderPort submissionReader;
    private final OfflineSubmissionWriterPort submissionWriter;
    private final OfflineSubmissionDecisionRecorder decisionRecorder;
    private final Clock clock;

    @Override
    @TchTx
    public Void handle(ApproveOfflineSubmissionCommand command) {
        var submission = submissionReader.getRequired(command.submissionId());
        var now = clock.instant();
        // TODO Phase E3+: publish OfflineSubmissionAdminApprovedEvent after commit so sales
        // re-attempts the ticket creation with a fresh promotionAttemptId.
        submissionWriter.save(submission.markAdminApproved(now));
        decisionRecorder.record(
            command.tenantId(), command.submissionId(), command.decidedBy(),
            OfflineSubmissionDecisionRecorder.DecisionType.APPROVE,
            command.reason(), now, false, null);
        log.info("offlinesync: submission {} admin-approved by {} (reason='{}')",
            command.submissionId(), command.decidedBy(), command.reason());
        return null;
    }
}
