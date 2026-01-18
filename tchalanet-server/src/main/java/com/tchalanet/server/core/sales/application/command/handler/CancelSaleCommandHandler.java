package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.core.autonomy.domain.AutonomyResolver;
import com.tchalanet.server.core.limitpolicy.application.facade.LimitPolicyFacade;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import com.tchalanet.server.core.sales.application.command.model.CancelSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.CancelSaleResult;
import com.tchalanet.server.core.sales.application.command.model.LimitNotice;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.session.application.port.out.PosSessionReaderPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelSaleCommandHandler implements CommandHandler<CancelSaleCommand, CancelSaleResult> {

    private final TicketReaderPort ticketReader;
    private final TicketWritterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    private final LimitPolicyFacade limitPolicyFacade;
    private final AutonomyResolver autonomyResolver;
    private final PosSessionReaderPort posSessionReaderPort;

    @Override
    @TchTx
    public CancelSaleResult handle(CancelSaleCommand cmd) {

        Ticket ticket =
            ticketReader
                .findWithLinesById(cmd.ticketId())
                .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

        Instant now = Instant.now(clock);

        // derive outletId if possible (for autonomy + limits context)
        var outletId =
            ticket.getSessionId() == null
                ? null
                : posSessionReaderPort.findById(ticket.getSessionId()).map(s -> s.outletId()).orElse(null);

        // limits + autonomy
        LimitEvaluationResult limitResult = evaluateCancelLimits(cmd, ticket, outletId, now);
        enforceCancelDecisionMatrix(cmd, ticket, outletId, limitResult, now);

        // domain transition
        ticket.voidTicket(now);

        // persist
        Ticket saved = ticketWriter.save(ticket);

        // event payload
        long totalStakeCents = cents(saved.getTotalAmount());
        String currency = saved.getCurrency() != null ? saved.getCurrency() : (cmd.currency() != null ? cmd.currency() : "HTG");

        var event =
            new TicketCancelledEvent(
                UUID.randomUUID(),
                now,
                saved.getTenantId(),
                saved.getId(),
                saved.getTerminalId(),
                saved.getSessionId(),
                cmd.performedBy().uuid(),
                cmd.reason(),
                totalStakeCents,
                currency,
                saved.getDrawId());

        AfterCommit.run(() -> publisher.publish(event));

        log.info("Ticket voided ticketId={} tenantId={} drawId={}", saved.getId(), saved.getTenantId(), saved.getDrawId());

        List<LimitNotice> warnings = toLimitNotices(limitResult);

        var outcome =
            limitResult.overallOutcome() == BreachOutcome.WARN
                ? CancelSaleResult.CancelOutcome.SUCCESS_WITH_WARNINGS
                : CancelSaleResult.CancelOutcome.SUCCESS;

        return new CancelSaleResult(saved, outcome, warnings);
    }

    private LimitEvaluationResult evaluateCancelLimits(
        CancelSaleCommand cmd, Ticket ticket, Object outletId, Instant now) {

        BigDecimal ticketStakeTotal = ticket.getTotalAmount();

        List<LimitContext.BetLine> betLines =
            ticket.getLines().stream()
                .map(l -> new LimitContext.BetLine(l.betType(), l.selection(), l.stake(), l.betOption()))
                .toList();

        LimitContext context =
            new LimitContext(
                cmd.tenantId(),
                ticket.getDrawId(),
                null, // drawChannelId
                AgentId.of(cmd.performedBy().uuid()),
                ticket.getTerminalId(),
                (com.tchalanet.server.common.types.id.OutletId) outletId, // cast if your type exists; else change signature
                null,
                List.of(),
                null,
                OperationType.CANCEL,
                betLines,
                ticketStakeTotal,
                betLines.size(),
                now,
                java.time.ZoneId.systemDefault());

        return limitPolicyFacade.evaluate(OperationType.CANCEL, context);
    }

    private void enforceCancelDecisionMatrix(
        CancelSaleCommand cmd,
        Ticket ticket,
        Object outletId,
        LimitEvaluationResult limitResult,
        Instant now) {

        if (limitResult.overallOutcome() == BreachOutcome.ALLOW) {
            return;
        }

        if (limitResult.overallOutcome() == BreachOutcome.WARN) {
            log.warn("Cancel limit WARN tenantId={} ticketId={} details={}",
                cmd.tenantId(), ticket.getId(), limitResult.details());
            return;
        }

        // BLOCK
        var autonomyPolicy =
            autonomyResolver.resolve(
                cmd.tenantId(),
                AgentId.of(cmd.performedBy().uuid()),
                ticket.getTerminalId(),
                (com.tchalanet.server.common.types.id.OutletId) outletId,
                now);

        List<LimitNotice> notices = toLimitNotices(limitResult);

        throw ProblemRest.limitBlocked(
            "Limit breach blocked",
            OperationType.CANCEL,
            notices,
            autonomyPolicy.requireApprovalOnBlock(),
            autonomyPolicy.approvalRole());
    }

    private static List<LimitNotice> toLimitNotices(LimitEvaluationResult limitResult) {
        if (limitResult == null || limitResult.details() == null) return List.of();
        return limitResult.details().stream()
            .map(d -> new LimitNotice(
                d.ruleKey(),
                d.outcome(),
                d.message(),
                d.targetApplied(),
                d.selectionKey(),
                d.currentValue(),
                d.limitValue()))
            .toList();
    }

    private static long cents(BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.movePointRight(2).longValue();
    }
}
