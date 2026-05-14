package com.tchalanet.server.core.offlinesync.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.offlinesync.api.command.ProcessOfflineBatchWithSalesCommand;
import com.tchalanet.server.core.offlinesync.api.command.ReceiveOfflineBatchCommand;
import com.tchalanet.server.core.offlinesync.api.command.ReceiveOfflineBatchResult;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineBatchWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCryptoPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.domain.event.OfflineBatchReadyForSalesEvent;
import com.tchalanet.server.core.offlinesync.internal.domain.event.OfflineBatchReceivedEvent;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineBatch;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineBatchStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineTechnicalRejectReason;
import com.tchalanet.server.core.offlinesync.internal.domain.service.OfflineGrantPolicy;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Concurrency contract (v1):
 * Offline batch reception is an audit-first operation — the batch is always persisted even
 * when the outlet is blocked or the day is closed (for audit purposes). Technical validation
 * (signature, hash, grant, sequence) is idempotent: duplicate batches are marked DUPLICATE
 * using a partial unique index on (tenant_id, batch_id) rather than an in-memory check.
 * No terminal-lock or session-status re-check is required here; those are enforced at
 * grant-issuance time and at Sales acceptance time.
 */
@UseCase
@RequiredArgsConstructor
public class ReceiveOfflineBatchCommandHandler
    implements CommandHandler<ReceiveOfflineBatchCommand, ReceiveOfflineBatchResult> {

  private static final OfflineGrantPolicy GRANT_POLICY = new OfflineGrantPolicy();

  private final OfflineCryptoPort crypto;
  private final OfflineBatchWriterPort batchWriter;
  private final OfflineGrantReaderPort grantReader;
  private final CommandBus commandBus;
  private final DomainEventPublisher events;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public ReceiveOfflineBatchResult handle(ReceiveOfflineBatchCommand cmd) {
    var now = Instant.now(clock);
    var grant = grantReader.findById(cmd.grantId())
        .filter(g -> g.tenantId().equals(cmd.tenantId()))
        .orElseThrow(() -> ProblemRest.forbidden("offline_grant.not_found"));

    if (!GRANT_POLICY.isUsable(grant, now)) {
      throw ProblemRest.forbidden("offline_grant.not_usable");
    }

    int technicalRejects = 0;
    int ready = 0;
    var batchId = OfflineBatchId.of(UUID.randomUUID());
    var submissions = new ArrayList<OfflineSaleSubmission>();

    for (var submission : cmd.submissions()) {
      boolean hashOk = crypto.verifyPayloadHash(submission.payloadJson(), submission.payloadHash());
      boolean signatureOk = crypto.verifyPayloadSignature(
          grant.terminalId(),
          submission.payloadJson(),
          submission.payloadHash(),
          submission.signature());
      var status = OfflineSubmissionStatus.READY_FOR_SALES;
      OfflineTechnicalRejectReason rejectReason = null;
      if (!hashOk || !signatureOk) {
        technicalRejects++;
        status = OfflineSubmissionStatus.TECHNICALLY_REJECTED;
        rejectReason = hashOk
            ? OfflineTechnicalRejectReason.INVALID_SIGNATURE
            : OfflineTechnicalRejectReason.PAYLOAD_HASH_MISMATCH;
      } else {
        ready++;
      }

      submissions.add(new OfflineSaleSubmission(
          OfflineSaleSubmissionId.of(idGenerator.newUuid()),
          cmd.tenantId(),
          batchId,
          cmd.grantId(),
          cmd.codeBatchId(),
          submission.offlineCode(),
          grant.terminalId(),
          grant.outletId(),
          grant.sellerUserId(),
          grant.salesSessionId(),
          submission.clientTicketId(),
          submission.localSequence(),
          submission.createdAtDevice(),
          now,
          submission.payloadJson(),
          submission.payloadHash(),
          submission.signature(),
          status,
          rejectReason,
          null,
          null,
          Set.of(),
          null));
    }

    var batchStatus = ready > 0 ? OfflineBatchStatus.READY_FOR_SALES : OfflineBatchStatus.TECHNICALLY_REJECTED;
    batchWriter.saveReceivedBatch(new OfflineBatch(
        batchId,
        cmd.tenantId(),
        grant.terminalId(),
        cmd.grantId(),
        cmd.codeBatchId(),
        cmd.clientBatchId(),
        now,
        batchStatus,
        cmd.submissions().size(),
        technicalRejects,
        0,
        0,
        0), submissions);
    int readyFinal = ready;

    AfterCommit.run(() -> {
      events.publish(new OfflineBatchReceivedEvent(
          EventId.of(idGenerator.newUuid()),
          now,
          cmd.tenantId(),
          batchId,
          grant.terminalId(),
          cmd.submissions().size()));

      if (readyFinal > 0) {
        events.publish(new OfflineBatchReadyForSalesEvent(
            EventId.of(idGenerator.newUuid()),
            now,
            cmd.tenantId(),
            batchId,
            readyFinal));
        commandBus.execute(new ProcessOfflineBatchWithSalesCommand(batchId));
      }
    });

    return new ReceiveOfflineBatchResult(
        batchId,
        batchStatus,
        cmd.submissions().size(),
        ready,
        technicalRejects);
  }
}
