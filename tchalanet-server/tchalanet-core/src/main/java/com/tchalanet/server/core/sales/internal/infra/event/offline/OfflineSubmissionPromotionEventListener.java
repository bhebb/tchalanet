package com.tchalanet.server.core.sales.internal.infra.event.offline;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionAdminApprovedEvent;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionTechValidatedEvent;
import com.tchalanet.server.core.sales.api.command.offline.CreateTicketFromOfflineSubmissionCommand;
import com.tchalanet.server.core.sales.api.command.offline.CreateTicketFromOfflineSubmissionResult;
import com.tchalanet.server.core.sales.api.event.OfflineSubmissionProcessedEvent;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.util.UUID;

/**
 * Subscribes to the two offline promotion-trigger events ({@code TechValidated} and
 * {@code AdminApproved}), dispatches a {@link CreateTicketFromOfflineSubmissionCommand}
 * to materialise the ticket, then publishes the {@link OfflineSubmissionProcessedEvent}
 * back so {@code core.offlinesync} can record the outcome.
 *
 * <p>Idempotence enforced via {@link ProcessedEventPort} with handler key
 * {@code "sales.offline-promotion"}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfflineSubmissionPromotionEventListener {

    static final String HANDLER_KEY = "sales.offline-promotion";

    private final CommandBus commandBus;
    private final DomainEventPublisher eventPublisher;
    private final ProcessedEventPort processedEventPort;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTechValidated(OfflineSubmissionTechValidatedEvent event) {
        promote(
            event.eventId(),
            new CreateTicketFromOfflineSubmissionCommand(
                event.tenantId(), event.submissionId(),
                event.grantId(), event.codeId(), event.offlineCode(),
                event.promotionAttemptId(), event.eventId(), event.draft()
            )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminApproved(OfflineSubmissionAdminApprovedEvent event) {
        promote(
            event.eventId(),
            new CreateTicketFromOfflineSubmissionCommand(
                event.tenantId(), event.submissionId(),
                event.grantId(), event.codeId(), event.offlineCode(),
                event.promotionAttemptId(), event.eventId(), event.draft()
            )
        );
    }

    private void promote(EventId sourceEventId, CreateTicketFromOfflineSubmissionCommand command) {
        if (!processedEventPort.markProcessedIfAbsent(HANDLER_KEY, sourceEventId.value())) {
            log.debug("sales: promotion already processed for event {}", sourceEventId);
            return;
        }

        CreateTicketFromOfflineSubmissionResult result = commandBus.execute(command);

        OfflineSubmissionProcessedEvent.Outcome outcome = switch (result.outcome()) {
            case PROMOTED -> OfflineSubmissionProcessedEvent.Outcome.PROMOTED;
            case BUSINESS_REJECTED -> OfflineSubmissionProcessedEvent.Outcome.BUSINESS_REJECTED;
            case DUPLICATE -> OfflineSubmissionProcessedEvent.Outcome.DUPLICATE;
        };

        var processed = new OfflineSubmissionProcessedEvent(
            EventId.of(UUID.randomUUID()),
            clock.instant(),
            command.tenantId(),
            command.submissionId(),
            command.promotionAttemptId(),
            outcome,
            result.ticketId(),
            result.rejectionCode(),
            result.rejectionReason()
        );
        AfterCommit.run(() -> eventPublisher.publish(processed));
    }
}
