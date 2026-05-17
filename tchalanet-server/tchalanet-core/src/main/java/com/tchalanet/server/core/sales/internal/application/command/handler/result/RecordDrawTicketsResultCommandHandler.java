package com.tchalanet.server.core.sales.internal.application.command.handler.result;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.drawresult.api.query.GetDrawResultProjectionByDrawIdQuery;
import com.tchalanet.server.core.sales.api.command.result.RecordDrawTicketsResultCommand;
import com.tchalanet.server.core.sales.api.command.result.RecordDrawTicketsResultResult;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.result.TicketWinningCalculator;
import com.tchalanet.server.core.sales.api.event.TicketResultedEvent;
import com.tchalanet.server.core.sales.internal.infra.cache.SalesTicketCacheEvictor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordDrawTicketsResultCommandHandler
    implements CommandHandler<RecordDrawTicketsResultCommand, RecordDrawTicketsResultResult> {

    private static final UserId SYSTEM_ACTOR = UserId.of(UUID.nameUUIDFromBytes(
        "sales:draw-result-applied".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final QueryBus queryBus;
    private final TicketWinningCalculator ticketWinningCalculator;
    private final DomainEventPublisher eventPublisher;
    private final SalesTicketCacheEvictor salesTicketCacheEvictor;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public RecordDrawTicketsResultResult handle(RecordDrawTicketsResultCommand command) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(command.tenantId(), "tenantId is required");
        Objects.requireNonNull(command.drawId(), "drawId is required");
        Objects.requireNonNull(command.drawResultId(), "drawResultId is required");
        var projection = queryBus.ask(new GetDrawResultProjectionByDrawIdQuery(command.drawId()));

        var tickets = ticketReader.findByDrawId(command.drawId());

        if (tickets.isEmpty()) {
            return new RecordDrawTicketsResultResult(0, 0, 0);
        }

        var now = clock.instant();

        var resultedEvents = new java.util.ArrayList<TicketResultedEvent>();
        var affectedTicketIds = new java.util.ArrayList<com.tchalanet.server.common.types.id.TicketId>();
        int skipped = 0;

        for (var ticket : tickets) {
            try {
                if (!ticket.identity().tenantId().equals(command.tenantId())) {
                    skipped++;
                    log.warn(
                        "sales.record-result.skip tenant mismatch ticketId={} ticketTenant={} commandTenant={}",
                        ticket.identity().id(),
                        ticket.identity().tenantId(),
                        command.tenantId()
                    );
                    continue;
                }

                if (ticket.lifecycle().result().status()
                    != com.tchalanet.server.core.sales.api.model.status.TicketResultStatus.NOT_RESULTED) {
                    skipped++;
                    continue;
                }

                var lineResults = ticketWinningCalculator.computeLineResults(ticket, projection);
                var updated = ticket.applyOfficialResult(lineResults, SYSTEM_ACTOR, now);
                var saved = ticketWriter.save(updated);

                affectedTicketIds.add(saved.identity().id());

                resultedEvents.add(new TicketResultedEvent(
                    EventId.of(idGenerator.newUuid()),
                    now,
                    saved.identity().tenantId(),
                    saved.identity().id(),
                    saved.lifecycle().result().status(),
                    saved.lifecycle().settlement().status(),
                    saved.winningAmount().amount(),
                    saved.money().currency().code(),
                    saved.context().outletId(),
                    saved.context().salesSessionId()
                ));
            } catch (Exception ex) {
                skipped++;
                log.warn(
                    "sales.record-result.skip ticketId={} drawId={} cause={}",
                    ticket.identity().id(),
                    command.drawId(),
                    ex.toString()
                );
            }
        }

        AfterCommit.run(() -> {
            resultedEvents.forEach(eventPublisher::publish);
            salesTicketCacheEvictor.evictByDraw(command.drawId());
            affectedTicketIds.forEach(salesTicketCacheEvictor::evictByTicket);
        });

        return new RecordDrawTicketsResultResult(tickets.size(), resultedEvents.size(), skipped);
    }
}
