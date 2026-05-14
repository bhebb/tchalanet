package com.tchalanet.server.core.sales.internal.application.service;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import com.tchalanet.server.core.limitpolicy.api.query.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitLineContext;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletLookupPort;
import com.tchalanet.server.core.sales.api.command.LimitNotice;
import com.tchalanet.server.core.sales.api.command.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.SellTicketLineInput;
import com.tchalanet.server.core.sales.internal.application.rule.DrawCutoffRule;
import com.tchalanet.server.core.sales.internal.domain.model.TicketLine;
import com.tchalanet.server.core.session.internal.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TicketSalePolicyService {

    private final SalesSessionReaderPort posSessionPort;
    private final OutletLookupPort outletLookupPort;
    private final DrawCutoffRule drawCutoffRule;
    private final QueryBus queryBus;
    private final TicketLinePreparationService ticketLinePreparationService;
    private final Clock clock;

    public record PreparedSale(
        SalesSession session,
        DrawSummary draw,
        Instant now,
        List<SellTicketLineInput> mergedLines,
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

        // TODO(sales-refactor): restore normalize/merge pipeline once legacy and v2 line models are reconciled.
        List<SellTicketLineInput> merged = command.lines();

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
                .orElseThrow(() -> ProblemRest.conflict("session.not_open"));

        if (outletLookupPort.isSalesBlocked(session.outletId())) {
            throw ProblemRest.conflict("outlet.sales_blocked");
        }
        return session;
    }

    private LimitEvaluationView evaluateLimitsAndAutonomy(
        SellTicketCommand command,
        SalesSession session,
        DrawSummary draw,
        List<SellTicketLineInput> mergedLines,
        Instant now) {

        var betLines =
            mergedLines.stream()
                .map(l -> new LimitLineContext(
                    l.betType(),
                    l.selection(),
                    l.stakeAmount().movePointRight(2).longValue(),
                    0L))
                .toList();

        LimitContext ctx =
            new LimitContext(
                command.tenantId(),
                session.outletId(),
                session.openedBy(),
                command.drawId(),
                null,
                now,
                betLines);

        LimitEvaluationView limitView = queryBus.ask(new EvaluateLimitPolicyQuery(ctx));

        // TODO(sales-refactor): re-enable autonomy resolution when new autonomy API is wired.
        if (limitView.outcome() == BreachOutcome.BLOCK) {
            List<LimitNotice> notices =
                limitView.breaches().stream()
                    .map(d -> new LimitNotice(
                        d.ruleKey().name(),
                        d.outcome(),
                        d.messageKey(),
                        d.appliedScope(),
                        null,
                        d.currentValue() == null ? null : java.math.BigDecimal.valueOf(d.currentValue()),
                        d.limitValue() == null ? null : java.math.BigDecimal.valueOf(d.limitValue())))
                    .toList();

            throw ProblemRest.conflict("Limit breach blocked");
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
