package com.tchalanet.server.core.sales.internal.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.core.autonomy.application.service.AutonomyResolutionService;
import com.tchalanet.server.core.autonomy.application.service.model.AutonomyResolveRequest;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.limitpolicy.application.query.model.evaluation.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.evaluation.LimitEvaluationView;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.sales.application.command.model.CancelSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.CancelSaleResult;
import com.tchalanet.server.core.sales.application.command.model.LimitNotice;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelSaleCommandHandler implements CommandHandler<CancelSaleCommand, CancelSaleResult> {

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    private final QueryBus queryBus;
    private final AutonomyResolutionService resolveAutonomyPolicyService;
    private final SalesSessionReaderPort posSessionReaderPort;
    private final DrawLookupPort drawLookupPort;

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
                : posSessionReaderPort.findById(ticket.getSessionId()).map(com.tchalanet.server.core.session.domain.model.SalesSession::outletId).orElse(null);

        // limits + autonomy
        LimitEvaluationView limitView = evaluateCancelLimits(cmd, ticket, outletId, now);
        enforceCancelDecisionMatrix(cmd, ticket, outletId, limitView, now);

        // domain transition
        ticket.voidTicket(now);

        // persist
        Ticket saved = ticketWriter.save(ticket);

        // event payload
        long totalStakeCents = cents(saved.getTotalAmount());
        String currency = saved.getCurrency() != null ? saved.getCurrency() : (cmd.currency() != null ? cmd.currency() : "HTG");

        var event =
            new TicketCancelledEvent(
                EventId.of(UUID.randomUUID()),
                now,
                saved.getTenantId(),
                saved.getId(),
                saved.getTerminalId(),
                saved.getSessionId(),
                cmd.performedBy(),
                cmd.reason(),
                totalStakeCents,
                currency,
                saved.getDrawId());

        AfterCommit.run(() -> publisher.publish(event));

        log.info("Ticket voided ticketId={} tenantId={} drawId={}", saved.getId(), saved.getTenantId(), saved.getDrawId());

        List<LimitNotice> warnings = toLimitNotices(limitView);

        var outcome =
            limitView.outcome() == com.tchalanet.server.common.types.enums.BreachOutcome.WARN
                ? CancelSaleResult.CancelOutcome.SUCCESS_WITH_WARNINGS
                : CancelSaleResult.CancelOutcome.SUCCESS;

        return new CancelSaleResult(saved, outcome, warnings);
    }

    private LimitEvaluationView evaluateCancelLimits(
        CancelSaleCommand cmd, Ticket ticket, Object outletId, Instant now) {

        BigDecimal ticketStakeTotal = ticket.getTotalAmount();

        List<LimitContext.BetLine> betLines =
            ticket.getLines().stream()
                .map(l -> new LimitContext.BetLine(l.betType(), l.selection(), l.stake(), l.betOption(), l.potentialPayout()))
                .toList();

        // Use tenant from the ticket (RLS context derived from request); avoid passing tenant from external command
        var scope = new com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef.TenantScope(ticket.getTenantId());

        var draw =
            drawLookupPort
                .findById(ticket.getDrawId())
                .orElseThrow(() -> ProblemRestException.notFound("Draw not found"));

        LimitContext context =
            new LimitContext(
                ticket.getTenantId(),
                ticket.getDrawId(),
                draw.drawChannelId()
                , com.tchalanet.server.common.types.id.AgentId.of(cmd.performedBy().value()),
                ticket.getTerminalId(),
                (com.tchalanet.server.common.types.id.OutletId) outletId, // cast if your type exists; else change signature
                null,
                List.of(),
                null,
                OperationType.CANCEL,
                scope,
                betLines,
                ticketStakeTotal,
                betLines.size(),
                now,
                drawZone(draw));

        return queryBus.ask(new EvaluateLimitPolicyQuery(context));
    }

    private ZoneId drawZone(Draw draw) {
        // L'entité Draw n'a pas de timezone, on utilise UTC par défaut
        // Le timezone est disponible dans DrawSummary si nécessaire
        return ZoneId.of("UTC");
    }

    private void enforceCancelDecisionMatrix(
        CancelSaleCommand cmd,
        Ticket ticket,
        Object outletId,
        LimitEvaluationView limitView,
        Instant now) {

        if (limitView.outcome() == com.tchalanet.server.common.types.enums.BreachOutcome.ALLOW) {
            return;
        }

        if (limitView.outcome() == com.tchalanet.server.common.types.enums.BreachOutcome.WARN) {
            log.warn("Cancel limit WARN tenantId={} ticketId={} details= {}", cmd.tenantId(), ticket.getId(), limitView.breaches());
            return;
        }

        // BLOCK
        var req = new AutonomyResolveRequest(
            com.tchalanet.server.common.types.id.AgentId.of(cmd.performedBy().value()),
            ticket.getTerminalId(),
            (com.tchalanet.server.common.types.id.OutletId) outletId,
            now);
        var autonomyPolicy = resolveAutonomyPolicyService.resolve(req);
        if (!autonomyPolicy.requireApprovalOnBlock()) {
            List<LimitNotice> notices =
                limitView.breaches().stream()
                    .map(d -> new LimitNotice(
                        d.ruleKey().name(),
                        d.outcome(),
                        d.messageKey(),
                        d.appliedTarget(),
                        d.code(),
                        d.currentValue(),
                        d.limitValue()))
                    .toList();

            throw ProblemRest.limitBlocked(
                "Limit breach blocked",
                com.tchalanet.server.common.types.enums.OperationType.CANCEL,
                notices,
                autonomyPolicy.requireApprovalOnBlock(),
                autonomyPolicy.approvalRole());
        }
    }

    private static List<LimitNotice> toLimitNotices(LimitEvaluationView limitView) {
        if (limitView == null || limitView.breaches() == null) return List.of();
        return limitView.breaches().stream()
            .map(d -> new LimitNotice(
                d.ruleKey().name(),
                d.outcome(),
                d.messageKey(),
                d.appliedTarget(),
                d.code(),
                d.currentValue(),
                d.limitValue()))
            .toList();
    }

    private static long cents(BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.movePointRight(2).longValue();
    }
}
