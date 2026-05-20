package com.tchalanet.server.core.sales.internal.application.command.handler.result;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.sales.api.command.result.ReconcileTicketsForCorrectedDrawResultCommand;
import com.tchalanet.server.core.sales.api.command.result.ReconcileTicketsForCorrectedDrawResultResult;
import com.tchalanet.server.core.sales.api.event.TicketResultCorrectedEvent;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.result.TicketWinningCalculator;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.infra.cache.SalesTicketCacheEvictor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ReconcileTicketsForCorrectedDrawResultCommandHandler
    implements CommandHandler<ReconcileTicketsForCorrectedDrawResultCommand, ReconcileTicketsForCorrectedDrawResultResult> {

    private static final UserId SYSTEM_ACTOR = UserId.of(UUID.nameUUIDFromBytes(
        "sales:draw-result-correction".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final DrawResultReaderPort drawResultReader;
    private final TicketWinningCalculator ticketWinningCalculator;
    private final DomainEventPublisher eventPublisher;
    private final SalesTicketCacheEvictor salesTicketCacheEvictor;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public ReconcileTicketsForCorrectedDrawResultResult handle(ReconcileTicketsForCorrectedDrawResultCommand command) {
        var projection = drawResultReader.findProjectionById(command.correctedDrawResultId())
            .orElseThrow(() -> new IllegalStateException(
                "Corrected draw result projection not found: " + command.correctedDrawResultId()));

        var tickets = ticketReader.findByDrawId(command.drawId());
        if (tickets.isEmpty()) {
            return new ReconcileTicketsForCorrectedDrawResultResult(0, 0, 0);
        }

        var now = clock.instant();

        var correctedEvents = new java.util.ArrayList<TicketResultCorrectedEvent>();
        var affectedTicketIds = new java.util.ArrayList<com.tchalanet.server.common.types.id.TicketId>();
        int skipped = 0;

        for (var ticket : tickets) {
            try {
                var lineResults = ticketWinningCalculator.computeLineResults(ticket, projection);

                Ticket updated;
                if (ticket.lifecycle().result().status() == TicketResultStatus.NOT_RESULTED) {
                    updated = ticket.applyOfficialResult(lineResults, SYSTEM_ACTOR, now);
                } else {
                    updated = ticket.overrideResult(lineResults, SYSTEM_ACTOR, command.reason(), now);
                }

                var saved = ticketWriter.save(updated);
                affectedTicketIds.add(saved.identity().id());
                correctedEvents.add(new TicketResultCorrectedEvent(
                    EventId.of(idGenerator.newUuid()),
                    now,
                    saved.identity().tenantId(),
                    command.drawId(),
                    saved.identity().id(),
                    command.previousDrawResultId(),
                    command.correctedDrawResultId(),
                    command.reason()
                ));
            } catch (Exception ex) {
                skipped++;
                log.warn("sales.reconcile.corrected-result.skip ticketId={} drawId={} cause={}",
                    ticket.identity().id(), command.drawId(), ex.toString());
            }
        }

        AfterCommit.run(() -> {
            correctedEvents.forEach(eventPublisher::publish);
            salesTicketCacheEvictor.evictByDraw(command.drawId());
            affectedTicketIds.forEach(salesTicketCacheEvictor::evictByTicket);
        });

        return new ReconcileTicketsForCorrectedDrawResultResult(
            tickets.size(),
            correctedEvents.size(),
            skipped
        );
    }
}
