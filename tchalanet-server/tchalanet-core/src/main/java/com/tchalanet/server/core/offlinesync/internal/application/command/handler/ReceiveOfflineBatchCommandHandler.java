package com.tchalanet.server.core.offlinesync.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.core.offlinesync.api.event.OfflineBatchReceivedEvent;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionReadyForSalesEvent;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionReceivedEvent;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.application.command.model.ReceiveOfflineBatchCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSalesGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmission;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReceiveOfflineBatchCommandHandler
    implements CommandHandler<ReceiveOfflineBatchCommand, OfflineBatchId> {

    private final OfflineSalesGrantReaderPort grantReader;
    private final OfflineSubmissionReaderPort submissionReader;
    private final OfflineSubmissionWriterPort submissionWriter;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public OfflineBatchId handle(ReceiveOfflineBatchCommand command) {
        var now = Instant.now(clock);
        var grant = grantReader.getById(command.grantId());

        if (!grant.isActive(now)) {
            throw new IllegalStateException("Grant is not active or expired");
        }

        var batchId = OfflineBatchId.of(idGenerator.newUuid());
        var persistedSubmissions = new ArrayList<OfflineSubmission>();
        var readyEvents = new ArrayList<OfflineSubmissionReadyForSalesEvent>();
        var receivedEvents = new ArrayList<OfflineSubmissionReceivedEvent>();

        for (var payload : command.submissions()) {
            var existing = submissionReader.findByGrantAndClientSaleId(
                command.grantId(), payload.clientSaleId());

            if (existing.isPresent()) {
                continue;
            }

            var submissionId = OfflineSaleSubmissionId.of(idGenerator.newUuid());
            var submission = new OfflineSubmission(
                submissionId,
                command.tenantId(),
                batchId,
                command.grantId(),
                payload.clientSaleId(),
                payload.payload(),
                OfflineSubmissionStatus.READY_FOR_SALES,
                null,
                null,
                null,
                0,
                null,
                now,
                null);

            persistedSubmissions.add(submission);

            receivedEvents.add(new OfflineSubmissionReceivedEvent(
                EventId.of(idGenerator.newUuid()), now, command.tenantId(),
                submissionId, batchId, payload.clientSaleId()));

            readyEvents.add(new OfflineSubmissionReadyForSalesEvent(
                EventId.of(idGenerator.newUuid()), now, command.tenantId(), submissionId));
        }

        submissionWriter.saveAll(persistedSubmissions);

        var batchEvent = new OfflineBatchReceivedEvent(
            EventId.of(idGenerator.newUuid()), now, command.tenantId(),
            batchId, command.grantId(), persistedSubmissions.size());

        AfterCommit.run(() -> {
            events.publish(batchEvent);
            receivedEvents.forEach(events::publish);
            readyEvents.forEach(events::publish);
        });

        return batchId;
    }
}
