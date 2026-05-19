package com.tchalanet.server.core.offlinesync.internal.application.command.handler.submission;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.core.offlinesync.api.command.submission.RecoverStuckOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionLineSnapshot;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionTechValidatedEvent;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionTicketDraft;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineEventOutboxPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionLineReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.code.OfflineCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Bumps the {@code promotionAttemptId} on a submission that didn't receive its return
 * event in time, then republishes a fresh {@link OfflineSubmissionTechValidatedEvent}
 * rebuilt from the persisted {@code offline_submission_line} rows.
 *
 * <p>The fresh event is recorded in the outbox table inside the same transaction; the
 * drainer picks it up shortly after commit. Any in-flight return for the previous attempt
 * is stale and will be ignored by {@code OfflineSyncPromotionPolicy.evaluateReturn}.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecoverStuckOfflineSubmissionCommandHandler
    implements CommandHandler<RecoverStuckOfflineSubmissionCommand, Void> {

    private final OfflineSubmissionReaderPort submissionReader;
    private final OfflineSubmissionWriterPort submissionWriter;
    private final OfflineSubmissionLineReaderPort submissionLineReader;
    private final OfflineGrantReaderPort grantReader;
    private final OfflineCodeReaderPort codeReader;
    private final OfflineEventOutboxPort eventOutbox;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public Void handle(RecoverStuckOfflineSubmissionCommand command) {
        var submission = submissionReader.getRequired(command.submissionId());
        var grant = grantReader.getRequired(submission.identity().grantId());

        OfflineCode code = codeReader
            .findByCode(grant.tenantId(), grant.id(), submission.identity().offlineCode())
            .orElseThrow(() -> com.tchalanet.server.common.web.error.ProblemRest.notFound(
                "offlinesync.code.not_found_for_recovery", submission.identity().offlineCode()));

        Instant now = clock.instant();
        var attempt = PromotionAttemptId.of(idGenerator.newUuid());
        var updated = submission.markPromotionRequested(attempt, now);
        submissionWriter.save(updated);

        List<OfflineSubmissionLineSnapshot> lines = submissionLineReader.findBySubmissionId(
            submission.id(), submission.payload().totalStakeAmount().currency());

        var draft = new OfflineSubmissionTicketDraft(
            grant.identity().sellerUserId(), grant.identity().terminalId(),
            grant.identity().outletId(), grant.identity().salesSessionId(),
            grant.device().deviceId(),
            submission.payload().drawId(),
            submission.payload().clientSoldAt(),
            submission.payload().totalStakeAmount(),
            submission.payload().lineCount(),
            submission.payload().payloadHash(),
            lines
        );

        var event = new OfflineSubmissionTechValidatedEvent(
            EventId.of(UUID.randomUUID()), now, grant.tenantId(),
            submission.id(), grant.id(), code.id(), code.identity().code(),
            attempt, draft
        );
        eventOutbox.record(event);

        log.info("offlinesync: republished tech-validated event for stuck submission {} (new attempt {})",
            command.submissionId(), attempt);
        return null;
    }
}
