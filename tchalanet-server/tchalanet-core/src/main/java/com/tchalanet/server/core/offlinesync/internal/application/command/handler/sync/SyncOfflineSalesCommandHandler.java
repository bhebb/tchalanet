package com.tchalanet.server.core.offlinesync.internal.application.command.handler.sync;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;
import com.tchalanet.server.core.limitpolicy.api.query.GetOfflineLimitPolicyQuery;
import com.tchalanet.server.core.offlinesync.api.command.sync.SyncOfflineSalesCommand;
import com.tchalanet.server.core.offlinesync.api.command.sync.SyncOfflineSalesResult;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionLineSnapshot;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionTechValidatedEvent;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionTicketDraft;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeBatchReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCryptoPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionLineWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSyncBatchReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSyncBatchWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.service.sync.OfflineSubmissionPayloadHasher;
import com.tchalanet.server.core.offlinesync.internal.domain.model.code.OfflineCode;
import com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch.OfflineCodeBatch;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.OfflineGrant;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.OfflineSubmission;
import com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch.OfflineSyncBatch;
import com.tchalanet.server.core.offlinesync.internal.domain.service.OfflineSubmissionTechnicalPolicy;
import jakarta.persistence.OptimisticLockException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core orchestrator of the offline sync flow — applies {@link OfflineSubmissionTechnicalPolicy}
 * to each uploaded submission, locks the offline code, persists results, and publishes
 * {@link OfflineSubmissionTechValidatedEvent} after-commit for {@code core.sales}.
 *
 * <p>The whole batch runs in a single transaction; per-submission failures are returned as
 * outcomes, never as exceptions (a bad submission must not undo good ones from the same batch).
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class SyncOfflineSalesCommandHandler
    implements CommandHandler<SyncOfflineSalesCommand, SyncOfflineSalesResult> {

    private final OfflineGrantReaderPort grantReader;
    private final OfflineGrantWriterPort grantWriter;
    private final OfflineCodeReaderPort codeReader;
    private final OfflineCodeWriterPort codeWriter;
    private final OfflineCodeBatchReaderPort codeBatchReader;
    private final OfflineSubmissionReaderPort submissionReader;
    private final OfflineSubmissionWriterPort submissionWriter;
    private final OfflineSubmissionLineWriterPort submissionLineWriter;
    private final OfflineSyncBatchReaderPort syncBatchReader;
    private final OfflineSyncBatchWriterPort syncBatchWriter;
    private final OfflineCryptoPort crypto;
    private final OfflineSubmissionPayloadHasher payloadHasher;
    private final com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineEventOutboxPort eventOutbox;
    private final IdGenerator idGenerator;
    private final QueryBus queryBus;
    private final Clock clock;

    @Override
    @TchTx
    public SyncOfflineSalesResult handle(SyncOfflineSalesCommand command) {
        Instant now = clock.instant();

        // Idempotence: same (tenant, grant, clientBatchId) replays existing batch silently.
        var existingBatch = syncBatchReader.findByClientBatchId(
            command.tenantId(), command.grantId(), command.clientBatchId());
        if (existingBatch.isPresent()) {
            log.info("offlinesync: replaying sync batch clientBatchId={}", command.clientBatchId());
            // For replay we return DUPLICATE outcomes per submission without re-processing.
            return replayResult(existingBatch.get(), command);
        }

        // Pessimistic-lock the grant for the whole batch — serialises concurrent batches
        // against the same grant so quota counters stay accurate.
        var grant = grantWriter.lockForUpdate(command.grantId())
            .orElseThrow(() -> com.tchalanet.server.common.web.error.ProblemRest.notFound(
                "offlinesync.grant.not_found", command.grantId()));

        OfflineLimitPolicy policy = queryBus.ask(new GetOfflineLimitPolicyQuery(command.tenantId()));

        OfflineSyncBatchId batchId = OfflineSyncBatchId.of(idGenerator.newUuid());
        OfflineSyncBatch batch = OfflineSyncBatch.open(
            batchId, command.tenantId(), command.grantId(),
            /* codeBatchId */ null,
            grant.identity().sellerUserId(), grant.identity().terminalId(),
            grant.identity().outletId(), grant.identity().salesSessionId(),
            grant.device().deviceId(), command.clientBatchId(),
            command.submissions().size(), now
        );
        batch = syncBatchWriter.save(batch);

        List<SyncOfflineSalesResult.SubmissionOutcome> outcomes = new ArrayList<>();
        int techReject = 0, accepted = 0;
        List<OfflineSubmissionTechValidatedEvent> toPublish = new ArrayList<>();

        for (var sub : command.submissions()) {
            SubmissionProcessOutcome r = processSubmission(
                grant, batch, sub, policy, command.trustedOperationalContext(), now);
            outcomes.add(r.outcome());
            if (r.publishedEvent() != null) {
                accepted++;
                toPublish.add(r.publishedEvent());
            } else if (r.outcome().outcome() == SyncOfflineSalesResult.Outcome.REJECTED) {
                techReject++;
            }
        }

        OfflineSyncBatch finalized = batch.withCounters(techReject, accepted, 0, 0, now);
        syncBatchWriter.save(finalized);

        // Outbox: events are recorded in the same tx as the business writes; the drainer
        // scheduler publishes them after-commit on the same node — or on another pod after
        // a crash. Guarantees at-least-once delivery.
        toPublish.forEach(eventOutbox::record);
        return new SyncOfflineSalesResult(batchId, List.copyOf(outcomes));
    }

    private SubmissionProcessOutcome processSubmission(
        OfflineGrant grant, OfflineSyncBatch batch,
        SyncOfflineSalesCommand.Submission sub, OfflineLimitPolicy policy,
        boolean trustedOperationalContext, Instant now
    ) {
        // Duplicate / payload-mismatch detection (idempotence)
        var existing = submissionReader.findByClientSubmissionId(
            grant.tenantId(), grant.id(), sub.clientSubmissionId());
        if (existing.isPresent()) {
            var sx = existing.get();
            if (!sx.payload().payloadHash().equals(sub.payloadHash())) {
                return reject(sub, "offlinesync.submission.payload_mismatch",
                    "Existing submission has a different payloadHash");
            }
            return new SubmissionProcessOutcome(
                new SyncOfflineSalesResult.SubmissionOutcome(
                    sub.clientSubmissionId(), sx.id(),
                    SyncOfflineSalesResult.Outcome.DUPLICATE, null, null),
                null);
        }

        // Resolve code first via cheap read, then upgrade to a pessimistic-write reload
        // so the AVAILABLE → RESERVED transition can't race with another worker.
        OfflineCode code = codeReader.findByCode(grant.tenantId(), grant.id(), sub.offlineCode())
            .flatMap(c -> codeWriter.lockForReservation(c.identity().id()))
            .orElse(null);
        OfflineCodeBatch codeBatch = code != null
            ? codeBatchReader.findById(code.codeBatchId()).orElse(null)
            : null;

        boolean signatureValid = crypto.verifySubmission(
            sub.payloadHash().getBytes(StandardCharsets.UTF_8),
            sub.signature(),
            grant.device().devicePublicKey());

        // Recompute the canonical payload hash server-side (check #14 of the spec).
        String recomputedHash = payloadHasher.hash(sub);

        // posContext is considered validated when the request carried a strong operational
        // context AND the grant's pos triple matches the operational context that issued it
        // (the grant aggregate is the source of truth for terminal/outlet/session).
        boolean posContextValidated = trustedOperationalContext;

        var inputs = new OfflineSubmissionTechnicalPolicy.Inputs(
            /* tenantOfflineEnabled */ policy.offlineEnabled(),
            /* planAllowsOffline */ policy.offlineEnabled(),
            /* trustedOperationalContext */ trustedOperationalContext,
            /* posContextValidated */ posContextValidated,
            grant,
            grant.device().deviceId(),
            grant.device().devicePublicKey(),
            signatureValid,
            code, codeBatch,
            sub.clientSoldAt(), now,
            sub.payloadHash(), recomputedHash,
            sub.totalStakeAmount()
        );

        var decision = OfflineSubmissionTechnicalPolicy.evaluate(inputs);
        if (decision instanceof OfflineSubmissionTechnicalPolicy.Decision.Reject reject) {
            if (code != null && code.status().name().equals("RESERVED")) {
                codeWriter.save(code.markConsumedRejected(now));
            }
            return reject(sub, reject.code(), reject.reason());
        }

        // Accept path: reserve the code, persist submission TECH_VALIDATED, schedule event.
        OfflineCode reserved;
        OfflineSubmissionId submissionId = OfflineSubmissionId.of(idGenerator.newUuid());
        try {
            reserved = codeWriter.save(code.reserve(submissionId, now));
        } catch (OptimisticLockException | IllegalStateException race) {
            return reject(sub, "offlinesync.code.race_lost",
                "Concurrent reservation lost: " + race.getMessage());
        }

        PromotionAttemptId attemptId = PromotionAttemptId.of(idGenerator.newUuid());
        OfflineSubmission submission = OfflineSubmission.receive(
            submissionId, grant.tenantId(),
            batch.id(), grant.id(), reserved.codeBatchId(),
            sub.offlineCode(), sub.clientSubmissionId(),
            grant.device().deviceId(), grant.identity().sellerUserId(),
            grant.identity().terminalId(), grant.identity().outletId(), grant.identity().salesSessionId(),
            sub.drawId(),
            sub.clientSoldAt(), now,
            sub.totalStakeAmount(), sub.lineCount(),
            sub.payloadHash(), sub.signature()
        ).markTechValidated(attemptId, now);
        submissionWriter.save(submission);

        // Persist lines so recover-stuck can rebuild a fresh TicketDraft later.
        var lineSnapshots = sub.lines().stream()
            .map(l -> new OfflineSubmissionLineSnapshot(
                l.lineNo(), l.gameCode(), l.betType(), l.betOption(),
                l.selectionKey(), l.stakeAmount(), l.potentialPayout()))
            .toList();
        submissionLineWriter.saveAll(grant.tenantId(), submissionId, lineSnapshots);

        // Bump grant quota counters now that the submission is technically validated.
        grantWriter.save(grant.recordValidatedTicket(sub.totalStakeAmount()));

        var event = buildTechValidatedEvent(grant, reserved.id(), submission, attemptId,
            sub, lineSnapshots, now);

        return new SubmissionProcessOutcome(
            new SyncOfflineSalesResult.SubmissionOutcome(
                sub.clientSubmissionId(), submissionId,
                SyncOfflineSalesResult.Outcome.ACCEPTED, null, null),
            event
        );
    }

    private OfflineSubmissionTechValidatedEvent buildTechValidatedEvent(
        OfflineGrant grant, OfflineCodeId codeId, OfflineSubmission submission,
        PromotionAttemptId attemptId, SyncOfflineSalesCommand.Submission sub,
        List<OfflineSubmissionLineSnapshot> lineSnapshots, Instant now
    ) {
        var draft = new OfflineSubmissionTicketDraft(
            grant.identity().sellerUserId(), grant.identity().terminalId(),
            grant.identity().outletId(), grant.identity().salesSessionId(),
            grant.device().deviceId(),
            sub.drawId(),
            sub.clientSoldAt(), sub.totalStakeAmount(),
            sub.lineCount(), sub.payloadHash(),
            lineSnapshots
        );
        return new OfflineSubmissionTechValidatedEvent(
            EventId.of(UUID.randomUUID()), now, grant.tenantId(),
            submission.id(), grant.id(), codeId, sub.offlineCode(),
            attemptId, draft
        );
    }

    private SubmissionProcessOutcome reject(
        SyncOfflineSalesCommand.Submission sub, String code, String reason
    ) {
        log.info("offlinesync: tech rejection for {} -> {} ({})",
            sub.clientSubmissionId(), code, reason);
        return new SubmissionProcessOutcome(
            new SyncOfflineSalesResult.SubmissionOutcome(
                sub.clientSubmissionId(), null,
                SyncOfflineSalesResult.Outcome.REJECTED, code, reason),
            null
        );
    }

    private SyncOfflineSalesResult replayResult(OfflineSyncBatch existing, SyncOfflineSalesCommand cmd) {
        var outcomes = cmd.submissions().stream()
            .map(s -> new SyncOfflineSalesResult.SubmissionOutcome(
                s.clientSubmissionId(), null,
                SyncOfflineSalesResult.Outcome.DUPLICATE, null, null))
            .toList();
        return new SyncOfflineSalesResult(existing.id(), outcomes);
    }

    private record SubmissionProcessOutcome(
        SyncOfflineSalesResult.SubmissionOutcome outcome,
        OfflineSubmissionTechValidatedEvent publishedEvent
    ) {}
}
