package com.tchalanet.server.core.offlinesync.internal.application.command.handler.submission;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.core.offlinesync.api.command.submission.ReleaseOrphanedReservedCodeCommand;
import com.tchalanet.server.core.offlinesync.api.model.code.OfflineCodeStatus;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ReleaseOrphanedReservedCodeCommandHandler
    implements CommandHandler<ReleaseOrphanedReservedCodeCommand, Void> {

    private final OfflineCodeReaderPort codeReader;
    private final OfflineCodeWriterPort codeWriter;
    private final OfflineSubmissionReaderPort submissionReader;
    private final OfflineSubmissionWriterPort submissionWriter;
    private final Clock clock;

    @Override
    @TchTx
    public Void handle(ReleaseOrphanedReservedCodeCommand command) {
        var code = codeReader.getRequired(command.codeId());
        if (code.status() != OfflineCodeStatus.RESERVED) {
            log.debug("offlinesync: code {} not RESERVED (status={}), skipping", code.id(), code.status());
            return null;
        }
        var now = clock.instant();
        codeWriter.save(code.markConsumedRejected(now));

        OfflineSubmissionId submissionId = code.lifecycle().offlineSubmissionId();
        if (submissionId != null) {
            submissionReader.findById(submissionId).ifPresent(s ->
                submissionWriter.save(s.markSyncFailed(
                    "offlinesync.code.orphaned_reservation",
                    "Code RESERVED past SLA without promotion outcome", now)));
        }
        log.warn("offlinesync: released orphaned RESERVED code {} (submission={})",
            code.id(), submissionId);
        return null;
    }
}
