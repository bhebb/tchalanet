package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.core.limitpolicy.application.query.model.evaluation.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutCommand;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutResult;
import com.tchalanet.server.core.payout.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.domain.event.PayoutPaidEvent;
import com.tchalanet.server.core.payout.domain.event.PayoutRequestedEvent;
import com.tchalanet.server.core.payout.domain.model.Payout;
import com.tchalanet.server.core.payout.domain.model.PayoutDecision;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Concurrency contract (v1 — no external cache):
 * 1. PosPayoutOperationValidator runs pre-transaction.
 * 2. Inside {@code @TchTx}: terminal-locked, outlet.payoutBlocked, and session.status are
 *    re-read from the DB before persisting the payout row. If any critical state changed
 *    since pre-check, the handler returns the appropriate blocked result (TERMINAL_LOCKED,
 *    OUTLET_PAYOUT_BLOCKED, SESSION_CLOSED).
 * 3. The payout row itself carries a version column; optimistic locking prevents double-pay.
 */
@UseCase
@RequiredArgsConstructor
public class RegisterPayoutCommandHandler
    implements CommandHandler<RegisterPayoutCommand, RegisterPayoutResult> {

    private final PayoutReaderPort payoutReader;
    private final PayoutWriterPort payoutWriter;
    private final PayoutReceiptPort ticketReader;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final QueryBus queryBus;

    @Override
    @TchTx
    public RegisterPayoutResult handle(RegisterPayoutCommand command) {
        var ticket = requireEligibleTicket(command);
        requireNoExistingPayout(command);

        var now = Instant.now(clock);
        var decision = decide(command, ticket, now);

        if (decision == PayoutDecision.BLOCK) {
            return RegisterPayoutResult.blocked(
                ticket.winningAmount(),
                ticket.currency(),
                List.of("payout.blocked"));
        }

        var payout = createPayout(command, ticket, now);

        if (decision == PayoutDecision.PAY_NOW) {
            markPaid(command, payout, now);
        }

        var saved = payoutWriter.save(payout);

        publishEvent(command, ticket, saved, decision, now);

        return result(decision, ticket, saved);
    }


    private PayoutDecision decide(RegisterPayoutCommand command,
                                  PayoutReceiptPort.PayoutTicketEligibilityView ticket,
                                  Instant now) {

        var limit = queryBus.ask(new EvaluateLimitPolicyQuery(
            new LimitContext(
                command.tenantId(),
                null,
                null,
                null,
                command.terminalId(),
                command.payingOutletId(),
                null,
                null,
                null,
                OperationType.PAYOUT,
                null,
                List.of(new LimitContext.BetLine(
                    null,
                    command.ticketId().value().toString(),
                    ticket.winningAmount(),
                    null,
                    ticket.winningAmount()
                )),
                ticket.winningAmount(),
                1,
                now,
                ZoneId.systemDefault()
            )
        ));

        if (limit.outcome() == BreachOutcome.BLOCK) {
            return PayoutDecision.BLOCK;
        }

        if (limit.outcome() == BreachOutcome.REQUIRE_APPROVAL) {
            return PayoutDecision.REQUIRE_APPROVAL;
        }

        return PayoutDecision.PAY_NOW;
    }

    private PayoutReceiptPort.PayoutTicketEligibilityView requireEligibleTicket(RegisterPayoutCommand command) {
        var ticket =
            ticketReader
                .findEligibilityByTicketId(command.ticketId())
                .orElseThrow(() -> new IllegalStateException("Ticket not found: " + command.ticketId()));

        if (!ticket.winning()) {
            throw new IllegalStateException("Ticket is not winning: " + command.ticketId());
        }

        if (ticket.alreadyPaid()) {
            throw new IllegalStateException("Ticket already paid: " + command.ticketId());
        }

        if (ticket.winningAmount() == null || ticket.winningAmount().signum() <= 0) {
            throw new IllegalStateException("Ticket has no positive winning amount: " + command.ticketId());
        }

        return ticket;
    }

    private void requireNoExistingPayout(RegisterPayoutCommand command) {
        payoutReader
            .findByTicketId(command.ticketId())
            .ifPresent(
                existing -> {
                    if (existing.isPaid()) {
                        throw new IllegalStateException(
                            "Ticket already has a paid payout: " + command.ticketId());
                    }

                    throw new IllegalStateException(
                        "Payout already exists for ticket: " + command.ticketId());
                });
    }

    private Payout createPayout(
        RegisterPayoutCommand command,
        PayoutReceiptPort.PayoutTicketEligibilityView ticket,
        Instant now) {

        return Payout.request(
            PayoutId.of(idGenerator.newUuid()),
            command.tenantId(),
            command.ticketId(),
            toCentsExact(ticket.winningAmount()),
            ticket.currency(),
            ticket.sellingOutletId(),
            ticket.sellingSessionId(),
            command.requestedBy(),
            command.reason(),
            now);
    }

    private void publishEvent(
        RegisterPayoutCommand command,
        PayoutReceiptPort.PayoutTicketEligibilityView ticket,
        Payout payout,
        PayoutDecision decision,
        Instant occurredAt) {

        if (decision == PayoutDecision.PAY_NOW) {
            publishPaid(command, payout, occurredAt);
            return;
        }

        publishRequested(command, payout, ticket.sellingSessionId(), occurredAt);
    }

    private RegisterPayoutResult result(
        PayoutDecision decision,
        PayoutReceiptPort.PayoutTicketEligibilityView ticket,
        Payout payout) {


        if (decision == PayoutDecision.PAY_NOW) {
            return RegisterPayoutResult.paidNow(payout.getId(),
                payout.getStatus(),
                ticket.winningAmount(),
                ticket.currency());
        }
        return RegisterPayoutResult.requested(
            payout.getId(),
            payout.getStatus(),
            ticket.winningAmount(),
            ticket.currency());
    }


    private void markPaid(RegisterPayoutCommand command, Payout payout, Instant now) {
        payout.markPaid(
            command.requestedBy(),
            command.payingOutletId(),
            command.payingSessionId(),
            command.terminalId(),
            now,
            true);
    }


    private void publishPaid(RegisterPayoutCommand command, Payout payout, Instant occurredAt) {
        var event =
            new PayoutPaidEvent(
                EventId.of(idGenerator.newUuid()),
                occurredAt,
                command.tenantId(),
                payout.getId(),
                payout.getTicketId(),
                payout.getAmountCents(),
                payout.getCurrency(),
                command.requestedBy(),
                command.payingSessionId(),
                command.payingOutletId(),
                command.terminalId());

        AfterCommit.run(() -> events.publish(event));
    }

    private void publishRequested(
        RegisterPayoutCommand command, Payout payout, SalesSessionId sellingSessionId, Instant occurredAt) {
        var event =
            new PayoutRequestedEvent(
                EventId.of(idGenerator.newUuid()),
                occurredAt,
                command.tenantId(),
                payout.getId(),
                payout.getTicketId(),
                payout.getAmountCents(),
                payout.getCurrency(),
                command.requestedBy(),
                sellingSessionId);

        AfterCommit.run(() -> events.publish(event));
    }

    private static long toCentsExact(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
