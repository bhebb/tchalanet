package com.tchalanet.server.core.sales.internal.application.command.handler.offline;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.command.offline.CreateTicketFromOfflineSubmissionCommand;
import com.tchalanet.server.core.sales.api.command.offline.CreateTicketFromOfflineSubmissionResult;
import com.tchalanet.server.core.sales.api.model.origin.OfflineSaleRef;
import com.tchalanet.server.core.sales.api.model.status.OfflineSyncStatus;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.offline.OfflineSubmissionToTicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;

/**
 * Materialises a {@link com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket}
 * from an offline submission promotionDecision request. Final defence against double promotionDecision is
 * the unique constraint {@code (tenant_id, offline_submission_id)} on {@code sales_ticket}.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateTicketFromOfflineSubmissionCommandHandler
    implements CommandHandler<CreateTicketFromOfflineSubmissionCommand,
                              CreateTicketFromOfflineSubmissionResult> {

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final OfflineSubmissionToTicketMapper ticketMapper;
    private final Clock clock;

    @Override
    @TchTx
    public CreateTicketFromOfflineSubmissionResult handle(
        CreateTicketFromOfflineSubmissionCommand command
    ) {
        // Fast path: if a ticket already exists for this submission, return DUPLICATE.
        var existing = ticketReader.findByOfflineSubmissionId(command.submissionId());
        if (existing.isPresent()) {
            log.info("sales: offline submission {} already promoted to ticket {}",
                command.submissionId(), existing.get().identity().id());
            return CreateTicketFromOfflineSubmissionResult.duplicate(existing.get().identity().id());
        }

        var draft = command.draft();
        var offlineRef = new OfflineSaleRef(
            command.submissionId(),
            null,                              // syncBatchId not carried by the event yet
            null,                              // codeBatchId not carried by the event yet
            command.offlineCode(),
            null,                              // clientSaleId — kept for backwards compat only
            0L,                                // localSequence — unused with current spec
            draft.clientSoldAt(),
            OfflineSyncStatus.ACCEPTED
        );

        try {
            var ticket = ticketMapper.toTicket(command.tenantId(), draft, offlineRef, clock.instant());
            var saved = ticketWriter.save(ticket);
            log.info("sales: promoted offline submission {} -> ticket {}",
                command.submissionId(), saved.identity().id());
            return CreateTicketFromOfflineSubmissionResult.promoted(saved.identity().id());
        } catch (DataIntegrityViolationException ex) {
            // Lost the race against another listener replay; load the winning ticket.
            return ticketReader.findByOfflineSubmissionId(command.submissionId())
                .map(t -> CreateTicketFromOfflineSubmissionResult.duplicate(t.identity().id()))
                .orElseGet(() -> CreateTicketFromOfflineSubmissionResult.businessRejected(
                    "sales.offline.unique_violation_no_lookup",
                    "Unique constraint hit but ticket lookup returned empty"));
        } catch (UnsupportedOperationException ex) {
            log.warn("sales: ticket construction not yet implemented for offline submission {} — {}",
                command.submissionId(), ex.getMessage());
            return CreateTicketFromOfflineSubmissionResult.businessRejected(
                "sales.offline.ticket_construction_not_implemented", ex.getMessage());
        } catch (java.util.NoSuchElementException | jakarta.persistence.EntityNotFoundException ex) {
            // The pinned drawId no longer exists (archived between sale and sync, or the
            // device sent an unknown id). Treat as business rejection — admin can review.
            log.warn("sales: cannot resolve draw for offline submission {} — {}",
                command.submissionId(), ex.getMessage());
            return CreateTicketFromOfflineSubmissionResult.businessRejected(
                "sales.offline.draw_not_resolved", ex.getMessage());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("sales: business rejection promoting offline submission {} — {}",
                command.submissionId(), ex.getMessage());
            return CreateTicketFromOfflineSubmissionResult.businessRejected(
                "sales.offline.business_rule_violation", ex.getMessage());
        }
    }
}
