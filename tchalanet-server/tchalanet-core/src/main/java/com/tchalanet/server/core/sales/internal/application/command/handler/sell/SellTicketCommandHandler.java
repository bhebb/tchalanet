package com.tchalanet.server.core.sales.internal.application.command.handler.sell;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecisionStatus;
import com.tchalanet.server.core.sales.api.event.TicketLinePlacedItem;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.payload.TicketContextPayload;
import com.tchalanet.server.core.sales.api.event.payload.TicketMoneyPayload;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketResult;
import com.tchalanet.server.core.sales.api.command.sell.SoldTicketView;
import com.tchalanet.server.core.sales.internal.application.port.out.AppliedPromotionSnapshotWriterPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketCodeGeneratorPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.receipt.TicketBackupAssembler;
import com.tchalanet.server.core.sales.internal.application.receipt.TicketReceiptAssembler;
import com.tchalanet.server.core.sales.internal.application.sale.SaleAcceptanceEvaluator;
import com.tchalanet.server.core.sales.internal.application.service.communication.TicketCommunicationRequestDispatcher;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketCodes;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketContext;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketIdentity;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.api.model.status.TicketPrintStatus;
import com.tchalanet.server.core.terminal.api.query.GetSellerTerminalForSaleValidationQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final TicketPrintReaderPort ticketPrintReader;
    private final DomainEventPublisher eventPublisher;
    private final SaleAcceptanceEvaluator saleAcceptanceEvaluator;
    private final AppliedPromotionSnapshotWriterPort appliedPromotionSnapshotWriter;
    private final TicketReceiptAssembler ticketReceiptAssembler;
    private final TicketBackupAssembler ticketBackupAssembler;
    private final TicketCommunicationRequestDispatcher communicationDispatcher;
    private final QueryBus queryBus;


    @Override
    @TchTx
    public SellTicketResult handle(SellTicketCommand command) {
        var ctx = TchContext.currentOrThrow();
        var tenantId = ctx.effectiveTenantIdRequired();
        var actorUserId = ctx.userId();
        var correlationId = ctx.correlationId();

        // 1. All business decisions in one place.
        var evaluation = saleAcceptanceEvaluator.evaluateFinal(command, ctx);
        var prepared = evaluation.preparedSale();
        if (!evaluation.acceptable()) {
            return new SellTicketResult(
                null,
                SellTicketOutcome.REJECTED,
                null,
                List.of(),
                evaluation.issues(),
                null,
                evaluation.actionAvailability(),
                evaluation.sellerInstruction()
            );
        }

        // 2. Build ticket context — branch on actor type.
        TicketContext ticketContext;
        if (ctx.actorType() == TchActorType.SELLER_TERMINAL) {
            var terminal = queryBus.ask(new GetSellerTerminalForSaleValidationQuery(
                tenantId, ctx.sellerTerminalId()));
            var commissionAmount = prepared.moneyBreakdown().stake().amount()
                .multiply(terminal.commissionRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            ticketContext = new TicketContext(
                null, null, null, null,
                command.drawId(), command.drawChannelId(),
                null, null,
                ctx.sellerTerminalId(), terminal.commissionRate(), commissionAmount
            );
        } else {
            ticketContext = new TicketContext(
                prepared.pos().outletId(),
                prepared.pos().terminalId(),
                prepared.pos().actorUserId(),
                prepared.pos().salesSessionId(),
                command.drawId(),
                command.drawChannelId(),
                prepared.sellerId(),
                prepared.sellerAssignmentId(),
                null, null, null
            );
        }

        // 3. Build and persist the aggregate.
        var ticketId = TicketId.of(idGenerator.newUuid());
        var ticket = Ticket.place(
            new TicketIdentity(ticketId, tenantId),
            ticketContext,
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

        var promo = prepared.promotionDecision();
        if (promo != null && promo.status() == PromotionDecisionStatus.APPLIED) {
            appliedPromotionSnapshotWriter.createIfAbsent(ticketId, promo, saved, prepared.now());
        }

        // Push the freshly persisted ticket to DB so the SQL view
        // (sales_ticket_print_header_v) used by the print reader sees it within the same tx.
        ticketWriter.flushPending();
        var receipt = ticketReceiptAssembler.assemble(
            ticketPrintReader.findPrintViewRequired(saved.identity().id()),
            ctx.locale()
        );
        var backup = ticketBackupAssembler.assemble(receipt);

        // 4. Publish AFTER COMMIT.
        AfterCommit.run(() -> {
            eventPublisher.publish(toTicketPlacedEvent(saved, prepared.now(), correlationId, prepared.promotionDecision()));

            communicationDispatcher.enqueueTicketPlaced(
                saved,
                command.communicationOptions(),
                correlationId
            );
        });

        // 5. Return result with notices propagated.
        var outcome = prepared.requiresApproval()
            ? SellTicketOutcome.PENDING_APPROVAL
            : SellTicketOutcome.ACCEPTED;

        return new SellTicketResult(
            toSoldTicketView(saved, backup.displayCode()),
            outcome,
            prepared.approvalRequestId(),
            prepared.notices(),
            evaluation.issues(),
            backup,
            evaluation.actionAvailability(),
            evaluation.sellerInstruction()
        );
    }

    private SoldTicketView toSoldTicketView(Ticket ticket, String displayCode) {
        return new SoldTicketView(
            ticket.identity().id(),
            ticket.codes().ticketCode().value(),
            ticket.codes().publicCode().value(),
            displayCode,
            ticket.codes().verificationCode().value(),
            ticket.lifecycle().sale().status(),
            ticket.lifecycle().result().status(),
            ticket.lifecycle().settlement().status(),
            ticket.origin().channel(),
            ticket.context().drawId(),
            ticket.context().outletId(),
            ticket.context().terminalId(),
            ticket.context().salesSessionId(),
            ticket.context().sellerUserId(),
            ticket.money().breakdown().total(),
            ticket.money().potentialPayoutAmount(),
            TicketPrintStatus.valueOf(ticket.print().status().name()),
            ticket.lifecycle().sale().soldAt(),
            ticket.lifecycle().sale().placedAt()
        );
    }

    private TicketPlacedEvent toTicketPlacedEvent(Ticket saved, Instant now, CorrelationId correlationId,
        com.tchalanet.server.core.promotion.api.model.PromotionDecision promotionDecision
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
            null,  // offlineRef: always null for POS_ONLINE flow
            promotionDecision
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
            line.potentialPayoutAmount(),
            line.origin(),
            line.pricingSource(),
            line.selectionSource(),
            line.payoutBaseAmount(),
            line.promotionDecisionId(),
            line.promotionLabel(),
            line.promotionEffectType()
        );
    }
}
