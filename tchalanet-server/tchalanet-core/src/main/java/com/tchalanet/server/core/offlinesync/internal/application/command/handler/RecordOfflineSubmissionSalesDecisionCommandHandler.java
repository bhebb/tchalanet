package com.tchalanet.server.core.offlinesync.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionSalesDecisionRecordedEvent;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.application.command.model.RecordOfflineSubmissionSalesDecisionCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RecordOfflineSubmissionSalesDecisionCommandHandler
    implements CommandHandler<RecordOfflineSubmissionSalesDecisionCommand, Void> {

    private final OfflineSubmissionReaderPort reader;
    private final OfflineSubmissionWriterPort writer;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public Void handle(RecordOfflineSubmissionSalesDecisionCommand command) {
        var submission = reader.getById(command.submissionId());

        if (submission.status() == OfflineSubmissionStatus.SALES_ACCEPTED
            || submission.status() == OfflineSubmissionStatus.SALES_REJECTED) {
            return null;
        }

        var now = Instant.now(clock);
        var updated = switch (command.decision()) {
            case SALES_ACCEPTED -> submission.markSalesAccepted(command.ticketId(), now);
            case SALES_REJECTED -> submission.markSalesRejected(command.rejectionCode(), now);
            default -> throw new IllegalArgumentException("Unexpected decision: " + command.decision());
        };

        writer.save(updated);

        var event = new OfflineSubmissionSalesDecisionRecordedEvent(
            EventId.of(idGenerator.newUuid()),
            now,
            command.tenantId(),
            command.submissionId(),
            updated.status(),
            updated.ticketId(),
            updated.salesRejectionCode());

        AfterCommit.run(() -> events.publish(event));
        return null;
    }
}
