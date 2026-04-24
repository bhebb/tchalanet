package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.application.command.model.SellTicketOutcome;
import com.tchalanet.server.core.sales.application.command.model.SellTicketResult;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.service.TicketSaleFactory;
import com.tchalanet.server.core.sales.domain.service.TicketSalePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SellTicketCommandHandler implements CommandHandler<SellTicketCommand, SellTicketResult> {

    private final TicketSalePolicy salePolicy;
    private final TicketSaleFactory ticketFactory;
    private final TicketWritterPort ticketWriter;
    private final DomainEventPublisher publisher;

    @Override
    @TchTx
    public SellTicketResult handle(SellTicketCommand command) {
        var prepared = salePolicy.prepareSale(command);

        var session = prepared.session(); // may be null depending on your design
        var now = prepared.now();

        // If your business requires session to sell, enforce here:
        // if (session == null) throw new DomainException("Session required to sell a ticket");

        if (prepared.limits().outcome() == BreachOutcome.BLOCK) {
            Ticket pending =
                ticketFactory.newPendingApprovalTicket(
                    command.tenantId(),
                    command.terminalId(),
                    session,
                    prepared.draw(),
                    prepared.ticketLines(),
                    now);

            var saved = ticketWriter.save(pending);

            var approvalRequestId = UUID.randomUUID(); // TODO: integrate approval domain later
            ApiResponseContext.get().addNotice(
                new ApiNotice(
                    "APPROVAL_REQUIRED",
                    "Transaction requires approval",
                    "sales",
                    NoticeSeverity.WARN,
                    Map.of("approvalRequestId", approvalRequestId)));

            return new SellTicketResult(saved, SellTicketOutcome.PENDING_APPROVAL, approvalRequestId);
        }

        Ticket sold =
            ticketFactory.newSoldTicket(
                command.tenantId(),
                command.terminalId(),
                session,
                prepared.draw(),
                prepared.ticketLines(),
                Currency.getInstance(command.currency()),
                now);

        var saved = ticketWriter.save(sold);

        // Optional: enforce "single game_code per ticket" for MVP
        // prepared.ticketLines is guaranteed non-empty by prepareSale()
        var primaryGame = prepared.ticketLines().stream().map(com.tchalanet.server.core.sales.domain.model.TicketLine::gameCode).findFirst().orElseThrow();
        boolean mixedGameCodes = prepared.ticketLines().stream().anyMatch(l -> !primaryGame.equals(l.gameCode()));
        if (mixedGameCodes) {
            // better to fail fast than publish ambiguous events
            throw new IllegalStateException("Mixed game_code per ticket is not supported yet (MVP)");
        }

        List<TicketPlacedEvent.Line> lines =
            prepared.ticketLines().stream()
                .map(l -> new TicketPlacedEvent.Line(
                    l.betType(),
                    l.selection(),
                    toCentsExact(l.stake()),
                    l.potentialPayout() == null ? 0L : toCentsExact(l.potentialPayout()),
                    l.betOption()
                ))
                .toList();

        var placed =
            new TicketPlacedEvent(
                EventId.of(UUID.randomUUID()),
                now,
                saved.getTenantId(),
                saved.getId(),
                session.outletId(),
                AgentId.of(session.userId().value()),
                command.terminalId(),
                session.id(),
                saved.getDrawId(),
                prepared.draw().drawChannel().id(),
                primaryGame.name(),
                toCentsExact(saved.getTotalAmount()),
                command.currency(),
                lines
            );

        AfterCommit.run(() -> publisher.publish(placed));

        var outcome =
            prepared.limits().outcome() == BreachOutcome.WARN
                ? SellTicketOutcome.SUCCESS_WITH_WARNINGS
                : SellTicketOutcome.SUCCESS;

        log.info("Ticket sold ticketId={} status={}", saved.getId(), saved.getSaleStatus());
        return new SellTicketResult(saved, outcome, null);
    }

    private static long toCentsExact(java.math.BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    }
}
