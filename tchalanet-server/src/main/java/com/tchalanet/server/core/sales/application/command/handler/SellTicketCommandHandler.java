package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.core.autonomy.domain.AutonomyResolver;
import com.tchalanet.server.core.draw.application.query.handler.GetDrawHandler;
import com.tchalanet.server.core.draw.application.query.model.GetDrawQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.limitpolicy.application.facade.LimitPolicyFacade;
import com.tchalanet.server.core.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import com.tchalanet.server.core.limitpolicy.domain.model.OperationType;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.application.command.model.SellTicketResult;
import com.tchalanet.server.core.sales.application.command.model.LimitNotice;
import com.tchalanet.server.core.sales.application.port.out.TicketNumberGeneratorPort;
import com.tchalanet.server.core.sales.application.port.out.TicketPublicCodeGeneratorPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.session.application.port.out.PosSessionReaderPort;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.tenant.domain.model.TenantId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.advice.ApiResponseContext;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SellTicketCommandHandler implements CommandHandler<SellTicketCommand, SellTicketResult> {

    // --- persistence ---
    private final TicketWritterPort ticketWriter;

    // --- generators ---
    private final TicketNumberGeneratorPort numberGenerator;
    private final TicketPublicCodeGeneratorPort publicCodeGenerator;

    // --- business rules / lookups ---
    private final PosSessionReaderPort posSessionPort;
    private final GetDrawHandler drawHandler;
    private final LimitPolicyFacade limitPolicyFacade;
    private final AutonomyResolver autonomyResolver;

    // --- events + time ---
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @TchTx
    public SellTicketResult handle(SellTicketCommand command) {

        // 1) Validate open session (security)
        PosSession session = validateSession(command.tenantId(), command.terminalId());

        // 2) Resolve draw + cutoff validation
        Draw draw = resolveAndValidateDraw(command);

        // 3) Limits and autonomy validation
        LimitEvaluationResult limitResult = validateLimitsAndAutonomy(command, session, draw);

        // 4) Build lines (odds snapshot placeholder for now, isolate here)
        List<TicketLine> lines = calculateLines(command.lines());

        // 5) Generate codes + create aggregate
        Instant now = Instant.now(clock);
        String ticketCode = numberGenerator.generate();
        String publicCode = publicCodeGenerator.generate();

        Ticket ticket =
            Ticket.create(
                command.tenantId(),
                command.terminalId(),
                session.id(),      // ✅ persist sessionId
                draw.id(),         // ✅ always use resolved draw
                ticketCode,
                publicCode,
                lines,
                now);

        // Check if we should persist
        if (limitResult.overallOutcome() == BreachOutcome.BLOCK) {
            // For BLOCK, we already handled in validateLimitsAndAutonomy, but since we return result, perhaps not persist
            // Wait, for BLOCK with approval, we should not persist ticket, return pending
            // But in validate, we throw for no approval, for approval we need to return pending without persisting
            // So, perhaps move the logic here.
        }

        // For now, assume we persist if not BLOCK
        if (limitResult.overallOutcome() != BreachOutcome.BLOCK) {
            // 6) Persist
            Ticket saved = ticketWriter.save(ticket);

            // 7) Publish domain event AFTER COMMIT
            TicketPlacedEvent event = buildTicketPlacedEvent(saved, session, lines, now, command.currency());
            AfterCommit.run(() -> domainEventPublisher.publish(event));

            log.info("Ticket sold ticketId={} tenantId={} publicCode={}", saved.getId(), saved.getTenantId(), saved.getPublicCode());

            List<ApiNotice> warnings = limitResult.details().stream()
                .map(d -> new ApiNotice(
                    "LIMIT_" + d.outcome().name(),
                    d.message(),
                    "limitpolicy",
                    d.outcome() == BreachOutcome.WARN ? NoticeSeverity.WARN : NoticeSeverity.INFO,
                    Map.of(
                        "ruleKey", d.ruleKey(),
                        "targetApplied", d.targetApplied(),
                        "selectionKey", d.selectionKey(),
                        "currentValue", d.currentValue(),
                        "limitValue", d.limitValue()
                    )
                ))
                .toList();

            // Add warnings to context
            warnings.forEach(ApiResponseContext.get()::addNotice);

            String status = limitResult.overallOutcome() == BreachOutcome.WARN ? "SUCCESS_WITH_WARNINGS" : "SUCCESS";
            return new SellTicketResult(saved, status, null);
        } else {
            // BLOCK - should not reach here if thrown, but for approval case
            // For now, return pending
            var approvalRequestId = UUID.randomUUID(); // dummy
            var notice = new ApiNotice(
                "APPROVAL_REQUIRED",
                "Transaction requires approval due to limit breach",
                "autonomy",
                NoticeSeverity.WARN,
                Map.of("approvalRequestId", approvalRequestId)
            );
            ApiResponseContext.get().addNotice(notice);
            return new SellTicketResult(null, "PENDING_APPROVAL", approvalRequestId);
        }
    }

    private PosSession validateSession(UUID tenantId, UUID terminalId) {
        return posSessionPort.findOpenByTerminal(tenantId, terminalId)
            .orElseThrow(() -> new SecurityException("No open session for terminalId=" + terminalId + " tenantId=" + tenantId));
    }

    private Draw resolveAndValidateDraw(SellTicketCommand command) {
        var draw = drawHandler.handle(new GetDrawQuery(command.tenantId(), command.drawId()));

        var cutoff = draw.cutoffAt().toInstant();
        var now = Instant.now(clock);
        if (now.isAfter(cutoff)) {
            throw new IllegalStateException("Draw cutoff time has passed for drawId=" + command.drawId());
        }
        return draw;
    }

    private LimitEvaluationResult validateLimitsAndAutonomy(SellTicketCommand command, PosSession session, Draw draw) {
        Instant now = Instant.now(clock);

        // Build limit context
        BigDecimal ticketStakeTotal = command.lines().stream()
            .map(SellTicketCommand.LineCommand::stake)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LimitContext.BetLine> betLines = command.lines().stream()
            .map(l -> new LimitContext.BetLine(l.betType(), l.selection(), l.stake()))
            .collect(Collectors.toList());

        LimitContext context = new LimitContext(
            command.tenantId(),
            draw.id(),
            null, // drawChannelId
            session.userId(), // agentId
            session.terminalId(),
            session.outletId(),
            null, // zoneId
            List.of(), // rangeIds
            null, // gameCode
            OperationType.SALE,
            betLines,
            ticketStakeTotal,
            betLines.size(),
            now,
            java.time.ZoneId.systemDefault()
        );

        // Evaluate limits
        LimitEvaluationResult limitResult = limitPolicyFacade.evaluate(OperationType.SALE, context);

        // Resolve autonomy
        var autonomyPolicy = autonomyResolver.resolve(command.tenantId(), session.userId(), session.terminalId(), session.outletId(), now);

        // Apply decision matrix V1
        if (limitResult.overallOutcome() == BreachOutcome.ALLOW) {
            // EXECUTE
        } else if (limitResult.overallOutcome() == BreachOutcome.WARN) {
            // EXECUTE + log
            log.warn("Limit breach (WARN) tenantId={} details={}", command.tenantId(), limitResult.details());
        } else if (limitResult.overallOutcome() == BreachOutcome.BLOCK) {
            if (!autonomyPolicy.requireApprovalOnBlock()) {
                // REJECT
                List<LimitNotice> notices = limitResult.details().stream()
                    .map(d -> new LimitNotice(d.ruleKey(), d.outcome(), d.message(), d.targetApplied().name(), d.selectionKey(), d.currentValue(), d.limitValue()))
                    .toList();
                throw ProblemRest.limitBlocked("Limit breach blocked", OperationType.SALE, notices, true, autonomyPolicy.approvalRole());
            }
            // else PENDING_APPROVAL - return result
        }
        return limitResult;
    }

    private List<TicketLine> calculateLines(List<SellTicketCommand.LineCommand> lineCommands) {
        if (lineCommands == null || lineCommands.isEmpty()) {
            throw new IllegalArgumentException("A ticket must have at least one line.");
        }

        return lineCommands.stream()
            .map(lineCmd -> {
                // TODO: replace with OddsSnapshotPort / PricingPort
                BigDecimal odds = new BigDecimal("10.00");
                BigDecimal potentialPayout = lineCmd.stake().multiply(odds);
                return new TicketLine(
                    lineCmd.gameCode(),
                    lineCmd.selection(),
                    lineCmd.stake(),
                    odds,
                    potentialPayout,
                    lineCmd.betType()
                );
            })
            .toList();
    }

    private TicketPlacedEvent buildTicketPlacedEvent(
        Ticket saved,
        PosSession session,
        List<TicketLine> lines,
        Instant occurredAt,
        String currency
    ) {
        String firstGameCode = lines.isEmpty() ? "" : lines.get(0).gameCode();

        long totalStakeCents = saved.getTotalAmount().movePointRight(2).longValue(); // safer than *100

        return new TicketPlacedEvent(
            UUID.randomUUID(),
            occurredAt,
            new TenantId(saved.getTenantId()),
            saved.getId(),
            null,              // outletId not in domain yet
            session.id(),      // sessionId
            session.userId(),  // cashierId (if that's what your session.userId means)
            saved.getDrawId(),
            firstGameCode,
            totalStakeCents,
            currency != null ? currency : "USD" // TODO: ideally tenant setting / command required
        );
    }
}
