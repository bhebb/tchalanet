package com.tchalanet.server.core.offlinesync.internal.application.command.handler.submission;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.command.submission.ReplayOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.command.submission.ReplayOfflineSubmissionResult;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Dry-run replay: re-loads a submission and reports what would happen if the policy was
 * re-applied. No state change. Useful for admin to inspect before issuing an actual
 * {@code ApproveOfflineSubmissionCommand}.
 *
 * <p>TODO Phase E3+: re-run {@code OfflineSubmissionTechnicalPolicy} against current grant /
 * code / quotas and return a detailed decision. For now returns a stub indicating the
 * submission is loadable.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class ReplayOfflineSubmissionCommandHandler
    implements CommandHandler<ReplayOfflineSubmissionCommand, ReplayOfflineSubmissionResult> {

    private final OfflineSubmissionReaderPort submissionReader;

    @Override
    @TchTx(readOnly = true)
    public ReplayOfflineSubmissionResult handle(ReplayOfflineSubmissionCommand command) {
        var submission = submissionReader.getRequired(command.submissionId());
        log.info("offlinesync: dry-run replay for submission {} (current status {})",
            submission.id(), submission.status());
        return new ReplayOfflineSubmissionResult(
            true,
            "offlinesync.replay.dry_run_not_implemented",
            "Dry-run policy re-evaluation not yet implemented; submission is loadable in status "
                + submission.status());
    }
}
