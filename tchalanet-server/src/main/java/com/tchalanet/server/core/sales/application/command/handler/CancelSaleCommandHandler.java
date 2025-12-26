package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.core.sales.application.command.model.CancelSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.CancelSaleResult;
import com.tchalanet.server.core.sales.application.command.model.LimitNotice;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tchalanet.server.core.autonomy.domain.AutonomyResolver;
import com.tchalanet.server.core.limitpolicy.application.facade.LimitPolicyFacade;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;

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

    @Override
    @TchTx
    public CancelSaleResult handle(CancelSaleCommand cmd) {
        var ticket = ticketReader.findWithLinesById(cmd.ticketId())
            .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

        // Limits and autonomy validation
        LimitEvaluationResult limitResult = validateLimitsAndAutonomy(cmd, ticket);

        var now = Instant.now(clock);
        ticket.voidTicket(now); // SOLD -> VOIDED
        var saved = ticketWriter.save(ticket);

        long totalStakeCents = saved.getTotalAmount().movePointRight(2).longValue();

        var event = new TicketCancelledEvent(
            UUID.randomUUID(),
            now,
            new com.tchalanet.server.common.types.id.TenantId(saved.getTenantId().uuid()),
            saved.getId(),
            saved.getTerminalId(),
            saved.getSessionId(),
            cmd.performedBy().uuid(),      // ou ctx user id
            cmd.reason(),
            totalStakeCents,
            cmd.currency()
        );

        AfterCommit.run(() -> publisher.publish(event));
        log.info("Ticket voided ticketId={} tenantId={}", saved.getId(), saved.getTenantId());

        List<LimitNotice> warnings = limitResult.details().stream()
            .map(d -> new LimitNotice(d.ruleKey(), d.outcome(), d.message(), d.targetApplied(), d.selectionKey(), d.currentValue(), d.limitValue()))
            .toList();

        String status = limitResult.overallOutcome() == BreachOutcome.WARN ? "SUCCESS_WITH_WARNINGS" : "SUCCESS";
        return new CancelSaleResult(saved, status, warnings);
    }

    private LimitEvaluationResult validateLimitsAndAutonomy(CancelSaleCommand cmd, Ticket ticket) {
        Instant now = Instant.now(clock);

        // Build limit context for cancel
        BigDecimal ticketStakeTotal = ticket.getTotalAmount();

        List<LimitContext.BetLine> betLines = ticket.getLines().stream()
            .map(l -> new LimitContext.BetLine(l.betType(), l.selection(), l.stake()))
            .collect(Collectors.toList());

        LimitContext context = new LimitContext(
            cmd.tenantId(),
            ticket.getDrawId(),
            null, // drawChannelId
            AgentId.of(cmd.performedBy().uuid()), // agentId
            ticket.getTerminalId(),
            null, // outletId - not available
            null, // zoneId
            List.of(), // rangeIds
            null, // gameCode
            OperationType.CANCEL,
            betLines,
            ticketStakeTotal,
            betLines.size(),
            now,
            java.time.ZoneId.systemDefault()
        );

        // Evaluate limits
        LimitEvaluationResult limitResult = limitPolicyFacade.evaluate(OperationType.CANCEL, context);

        // Resolve autonomy

        // Apply decision matrix V1
        if (limitResult.overallOutcome() == BreachOutcome.ALLOW) {
            // EXECUTE
        } else if (limitResult.overallOutcome() == BreachOutcome.WARN) {
            // EXECUTE + log
            log.warn("Limit breach (WARN) tenantId={} details={}", cmd.tenantId(), limitResult.details());
        } else if (limitResult.overallOutcome() == BreachOutcome.BLOCK) {
            var autonomyPolicy = autonomyResolver.resolve(cmd.tenantId(), AgentId.of(cmd.performedBy().uuid()), ticket.getTerminalId(), null, now);
            // REJECT - for cancel, always block if limits breached
            List<LimitNotice> notices = limitResult.details().stream()
                .map(d -> new LimitNotice(d.ruleKey(), d.outcome(), d.message(), d.targetApplied(), d.selectionKey(), d.currentValue(), d.limitValue()))
                .toList();
            throw ProblemRest.limitBlocked("Limit breach blocked", OperationType.CANCEL, notices, autonomyPolicy.requireApprovalOnBlock(), autonomyPolicy.approvalRole());
        }
        return limitResult;
    }
}
