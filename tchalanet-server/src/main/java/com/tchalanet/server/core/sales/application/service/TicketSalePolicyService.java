package com.tchalanet.server.core.sales.application.service;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.autonomy.application.service.ResolveAutonomyPolicyService;
import com.tchalanet.server.core.autonomy.application.service.model.AutonomyResolveRequest;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.limitpolicy.application.query.model.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.LimitEvaluationView;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import com.tchalanet.server.core.outlet.application.port.out.OutletLookupPort;
import com.tchalanet.server.core.sales.application.command.model.LimitNotice;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.application.rule.DrawCutoffRule;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TicketSalePolicyService {

    private final SalesSessionReaderPort posSessionPort;
    private final OutletLookupPort outletLookupPort;
    private final DrawCutoffRule drawCutoffRule;
    private final QueryBus queryBus;
    private final ResolveAutonomyPolicyService resolveAutonomyPolicyService;
    private final TicketLinePreparationService ticketLinePreparationService;
    private final Clock clock;

    public record PreparedSale(
        SalesSession session,
        DrawSummary draw,
        Instant now,
        List<SellTicketCommand.LineCommand> mergedLines,
        List<TicketLine> ticketLines,
        LimitEvaluationView limits) {
    }

    /**
     * Shared pipeline used by SELL and APPROVE.
     * - Validates session + draw cutoff
     * - Normalizes/merges lines
     * - Evaluates limits/autonomy
     * - Builds TicketLine snapshots (odds placeholder)
     */
    public PreparedSale prepareSale(SellTicketCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new IllegalArgumentException("A ticket must have at least one line.");
        }

        var now = Instant.now(clock);

        var session = validateSession(command.tenantId(), command.terminalId());
        var draw = drawCutoffRule.requireBeforeCutoff(command.drawId());

        List<SellTicketCommand.LineCommand> normalized = ticketLinePreparationService.normalize(command.lines());
        List<SellTicketCommand.LineCommand> merged = ticketLinePreparationService.mergeDuplicates(normalized);

        var limits = evaluateLimitsAndAutonomy(command, session, draw, merged, now);

        var ticketLines = ticketLinePreparationService.toTicketLines(command.tenantId(), merged);

        return new PreparedSale(session, draw, now, merged, ticketLines, limits);
    }

    /**
     * For APPROVE: re-check cutoff and optionally re-check limits.
     */
    public DrawSummary resolveAndValidateDraw(DrawId drawId) {
        return drawCutoffRule.requireBeforeCutoff(drawId);
    }

    public SalesSession validateSession(TenantId tenantId, TerminalId terminalId) {
        SalesSession session =
            posSessionPort
                .findOpenByTerminal(tenantId, terminalId)
                .orElseThrow(() -> new SecurityException("No open session for terminalId=" + terminalId));

        if (outletLookupPort.isSalesBlocked(session.outletId())) {
            throw new SecurityException("Outlet sales are temporarily blocked for outletId=" + session.outletId());
        }
        return session;
    }

    private LimitEvaluationView evaluateLimitsAndAutonomy(
        SellTicketCommand command,
        SalesSession session,
        DrawSummary draw,
        List<SellTicketCommand.LineCommand> mergedLines,
        Instant now) {

        BigDecimal total =
            mergedLines.stream().map(SellTicketCommand.LineCommand::stake).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LimitContext.BetLine> betLines =
            mergedLines.stream().map(l -> new LimitContext.BetLine(l.betType(), l.selection(), l.stake(), l.betOption(), BigDecimal.ZERO)).collect(Collectors.toList());

        LimitScopeRef scope = new LimitScopeRef.TenantScope(command.tenantId());

        LimitContext ctx =
            new LimitContext(
                command.tenantId(),
                draw.drawId(),
                null,
                AgentId.of(session.userId().value()),
                session.terminalId(),
                session.outletId(),
                null,
                List.of(),
                null,
                OperationType.SALE,
                scope,
                betLines,
                total,
                betLines.size(),
                now,
                drawZone(draw));

        LimitEvaluationView limitView = queryBus.send(new EvaluateLimitPolicyQuery(ctx));

        var autonomyReq = new AutonomyResolveRequest(
            AgentId.of(session.userId().value()),
            session.terminalId(),
            session.outletId(),
            now);

        var autonomyPolicy = resolveAutonomyPolicyService.resolve(autonomyReq);

        if (limitView.outcome() == BreachOutcome.BLOCK && !autonomyPolicy.requireApprovalOnBlock()) {
            List<LimitNotice> notices =
                limitView.breaches().stream()
                    .map(d -> new LimitNotice(
                        d.ruleKey().name(),
                        d.outcome(),
                        d.messageKey(),
                        d.appliedTarget(),
                        null,
                        d.currentValue(),
                        d.limitValue()))
                    .toList();

            throw com.tchalanet.server.common.error.ProblemRest.limitBlocked(
                "Limit breach blocked", OperationType.SALE, notices, true, autonomyPolicy.approvalRole());
        }

        return limitView;
    }

    private ZoneId drawZone(DrawSummary draw) {
        if (draw == null || draw.resultTimezone() == null) {
            return ZoneId.of("UTC");
        }
        return ZoneId.of(draw.resultTimezone());
    }
}
