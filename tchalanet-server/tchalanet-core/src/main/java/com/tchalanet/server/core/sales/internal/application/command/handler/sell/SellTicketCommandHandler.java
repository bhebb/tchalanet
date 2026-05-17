package com.tchalanet.server.core.sales.internal.application.command.handler.sell;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.event.TicketLinePlacedItem;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.payload.TicketContextPayload;
import com.tchalanet.server.core.sales.api.event.payload.TicketMoneyPayload;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketResult;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketCodeGeneratorPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.communication.TicketCommunicationRequestDispatcher;
import com.tchalanet.server.core.sales.internal.application.service.sell.TicketSalePolicyService;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketCodes;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketContext;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketIdentity;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SellTicketCommandHandler
    implements CommandHandler<SellTicketCommand, SellTicketResult> {

    private final IdGenerator idGenerator;
    private final TicketCodeGeneratorPort ticketCodeGenerator;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher eventPublisher;
    private final TicketSalePolicyService ticketSalePolicyService;
    private final TicketCommunicationRequestDispatcher communicationDispatcher;


    @Override
    @TchTx
    public SellTicketResult handle(SellTicketCommand command) {
        var ctx = TchContext.currentOrThrow();
        var tenantId = ctx.effectiveTenantIdRequired();
        var actorUserId = ctx.userId();
        var correlationId = ctx.correlationId();

        // 1. All business decisions in one place.
        var prepared = ticketSalePolicyService.prepareSale(command, ctx);

        // 2. Build and persist the aggregate.
        var ticketId = TicketId.of(idGenerator.newUuid());
        var ticket = Ticket.place(
            new TicketIdentity(ticketId, tenantId),
            new TicketContext(
                prepared.pos().outletId(),
                prepared.pos().terminalId(),
                prepared.pos().actorUserId(),
                prepared.pos().salesSessionId(),
                command.drawId(),
                command.drawChannelId()
            ),
            new TicketCodes(
                ticketCodeGenerator.nextTicketCode(),
                ticketCodeGenerator.nextPublicCode(),
                ticketCodeGenerator.nextVerificationCode()
            ),
            prepared.moneyBreakdown(),
            prepared.ticketLines(),
            TicketSaleChannel.POS_ONLINE,
            null,
            prepared.requiresApproval(),
            prepared.approvalRequestId(),
            actorUserId,
            prepared.now()
        );

        var saved = ticketWriter.save(ticket);

        // 3. Publish AFTER COMMIT.
        AfterCommit.run(() -> {
            eventPublisher.publish(toTicketPlacedEvent(saved, prepared.now(), correlationId));

            communicationDispatcher.enqueueTicketPlaced(
                saved,
                command.communicationOptions(),
                correlationId
            );
        });

        // 4. Return result with notices propagated.
        var outcome = prepared.requiresApproval()
            ? SellTicketOutcome.PENDING_APPROVAL
            : SellTicketOutcome.SOLD;

        return new SellTicketResult(
            saved,
            outcome,
            prepared.approvalRequestId(),
            prepared.notices()
        );
    }

    private TicketPlacedEvent toTicketPlacedEvent(Ticket saved, Instant now, CorrelationId correlationId
    ) {
        var context = new TicketContextPayload(
            saved.context().outletId(),
            saved.context().terminalId(),
            saved.context().sellerUserId(),
            saved.context().salesSessionId(),
            saved.context().drawId(),
            saved.context().drawChannelId()
        );

        var chargeItems = saved.money().breakdown().charges().stream()
            .map(c -> new TicketMoneyPayload.ChargeItem(c.type(), c.amount(), c.paidBy()))
            .toList();

        var money = new TicketMoneyPayload(
            saved.money().currency(),
            saved.money().breakdown().stake(),
            saved.money().breakdown().total(),
            saved.money().potentialPayoutAmount(),
            chargeItems
        );

        List<TicketLinePlacedItem> lines = saved.lines().stream()
            .map(this::toLineItem)
            .toList();

        return new TicketPlacedEvent(
            EventId.of(idGenerator.newUuid()),
            TicketPlacedEvent.CURRENT_SCHEMA,
            now,
            correlationId,
            saved.identity().tenantId(),
            saved.identity().id(),
            saved.lifecycle().sale().status(),
            saved.origin().channel(),
            context,
            money,
            lines,
            null  // offlineRef: always null for POS_ONLINE flow
        );
    }

    private TicketLinePlacedItem toLineItem(TicketLine line) {
        return new TicketLinePlacedItem(
            line.id(),
            line.lineNumber(),
            line.gameCode(),
            line.betType(),
            line.selection().key().value(),
            line.selection().displayLabel(),
            line.betOption(),
            line.stakeAmount(),
            line.oddsSnapshot(),
            line.potentialPayoutAmount()
        );
    }
}
