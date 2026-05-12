package com.tchalanet.server.core.offlinesync.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.offlinesync.api.command.ApproveOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.command.ApproveOfflineSubmissionResult;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.event.OfflineSubmissionSalesDecisionRecordedEvent;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineDecision;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/**
 * Concurrency contract (v1):
 * Admin approval is idempotent on SALES_REVIEW_REQUIRED submissions only. The writer port
 * uses optimistic locking on the submission row; a concurrent second approval returns
 * SALES_ACCEPTED and is a no-op. Actual ticket creation is deferred: this handler records
 * the ACCEPTED decision and publishes OfflineSubmissionSalesDecisionRecordedEvent; a
 * core.sales listener creates the ticket asynchronously (see §21).
 */
@UseCase
@RequiredArgsConstructor
public class ApproveOfflineSubmissionCommandHandler
    implements CommandHandler<ApproveOfflineSubmissionCommand, ApproveOfflineSubmissionResult> {

  private final OfflineSubmissionReaderPort reader;
  private final OfflineSubmissionWriterPort writer;
  private final DomainEventPublisher events;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public ApproveOfflineSubmissionResult handle(ApproveOfflineSubmissionCommand cmd) {
    var submission = reader.findById(cmd.submissionId())
        .orElseThrow(() -> new IllegalArgumentException("Offline submission not found: " + cmd.submissionId()));

    requireReviewRequired(submission);

    writer.recordSalesDecision(cmd.submissionId(), SalesOfflineDecision.ACCEPTED, null, null);

    var now = Instant.now(clock);
    var riskFlags = submission.riskFlags() != null ? submission.riskFlags() : Set.of();

    AfterCommit.run(() -> events.publish(new OfflineSubmissionSalesDecisionRecordedEvent(
        EventId.of(idGenerator.newUuid()),
        now,
        submission.tenantId(),
        submission.batchId(),
        submission.id(),
        SalesOfflineDecision.ACCEPTED,
        null,
        riskFlags,
        null
    )));

    // Ticket creation: a core.sales listener on OfflineSubmissionSalesDecisionRecordedEvent
    // will create the TicketPlacedEvent — see TODO in core.sales infra event package.
    return new ApproveOfflineSubmissionResult(cmd.submissionId(), null);
  }

  private static void requireReviewRequired(OfflineSaleSubmission submission) {
    if (submission.status() != OfflineSubmissionStatus.SALES_REVIEW_REQUIRED) {
      throw new IllegalStateException(
          "Submission is not in SALES_REVIEW_REQUIRED status: " + submission.id()
              + " (current: " + submission.status() + ")");
    }
  }
}
