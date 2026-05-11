package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.application.command.model.ProcessOfflineBatchWithSalesCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.ReceiveOfflineBatchCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.ReceiveOfflineBatchResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineBatchWriterPort;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineCryptoPort;
import com.tchalanet.server.core.offlinesync.domain.event.OfflineBatchReadyForSalesEvent;
import com.tchalanet.server.core.offlinesync.domain.event.OfflineBatchReceivedEvent;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineBatch;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineBatchStatus;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
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

  private final OfflineCryptoPort crypto;
  private final OfflineBatchWriterPort batchWriter;
  private final CommandBus commandBus;
  private final DomainEventPublisher events;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public ReceiveOfflineBatchResult handle(ReceiveOfflineBatchCommand cmd) {
    int technicalRejects = 0;
    int ready = 0;

    for (var submission : cmd.submissions()) {
      boolean hashOk = crypto.verifyPayloadHash(submission.payloadJson(), submission.payloadHash());
      boolean signatureOk = crypto.verifyPayloadSignature(cmd.terminalId(), submission.payloadJson(), submission.payloadHash(), submission.signature());
      if (!hashOk || !signatureOk) {
        technicalRejects++;
      } else {
        ready++;
      }
    }

    var batchStatus = ready > 0 ? OfflineBatchStatus.READY_FOR_SALES : OfflineBatchStatus.TECHNICALLY_REJECTED;
    var batchId = batchWriter.saveReceivedBatch(new OfflineBatch(
        OfflineBatchId.of(UUID.randomUUID()),
        cmd.tenantId(),
        cmd.terminalId(),
        cmd.grantId(),
        cmd.codeBatchId(),
        cmd.clientBatchId(),
        Instant.now(clock),
        batchStatus,
        cmd.submissions().size(),
        technicalRejects,
        0,
        0,
        0));
    var now = Instant.now(clock);
    int readyFinal = ready;

    AfterCommit.run(() -> {
      events.publish(new OfflineBatchReceivedEvent(
          EventId.of(idGenerator.newUuid()),
          now,
          cmd.tenantId(),
          batchId,
          cmd.terminalId(),
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
