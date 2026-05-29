package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.autonomy.api.query.ResolveAutonomyQuery;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitLineContext;
import com.tchalanet.server.core.limitpolicy.api.query.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SalePolicyDecision;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SalePolicyInput;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import com.tchalanet.server.platform.identity.api.model.AutonomyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


@Component
@RequiredArgsConstructor
public class SaleLimitAutonomyEvaluator {

    private final QueryBus queryBus;

    public SalePolicyDecision evaluate(
        TenantId tenantId,
        SellTicketCommand command,
        ValidatedPosOperationContext pos,
        DrawSummary draw,
        SalePolicyInput policyInput,
        Instant now
    ) {
        var limitContext = toLimitContext(tenantId, command, pos, draw, policyInput, now);
        var limits = queryBus.ask(new EvaluateLimitPolicyQuery(limitContext));

        if (limits == null) {
            throw ProblemRest.conflict("sales.limit_evaluation_failed");
        }

        if (limits.outcome() == null) {
            return SalePolicyDecision.allowed(limits);
        }

        return switch (limits.outcome()) {
            case ALLOW -> SalePolicyDecision.allowed(limits);
            case WARN -> SalePolicyDecision.allowedWithWarning(limits);
            case REQUIRE_APPROVAL -> resolveWithAutonomy(tenantId, pos, limits);
            case BLOCK -> throw ProblemRest.conflict("sales.limit_blocked");
        };
    }

    private SalePolicyDecision resolveWithAutonomy(
        TenantId tenantId,
        ValidatedPosOperationContext pos,
        LimitEvaluationView limits
    ) {
        var autonomy = queryBus.ask(new ResolveAutonomyQuery(
            tenantId,
            pos.outletId(),
            pos.actorUserId(),
            limits.outcome()
        ));

        if (autonomy == null || autonomy.autonomyLevel() == null) {
            throw ProblemRest.conflict("sales.autonomy_resolution_failed");
        }

        if (autonomy.autonomyLevel() == AutonomyLevel.NONE) {
            throw ProblemRest.conflict("sales.limit_blocked");
        }

        return SalePolicyDecision.requiresApproval(limits, autonomy.autonomyLevel());
    }

    private LimitContext toLimitContext(
        TenantId tenantId,
        SellTicketCommand command,
        ValidatedPosOperationContext pos,
        DrawSummary draw,
        SalePolicyInput policyInput,
        Instant now
    ) {
        var finalBasis = policyInput.finalBasis().isEmpty()
            ? policyInput.paidBasis().stream().map(this::toLimitLineContext).toList()
            : policyInput.finalBasis().stream().map(this::toLimitLineContext).toList();

        var lineContexts = finalBasis.stream()
            .toList();

        return new LimitContext(
            tenantId,
            pos.outletId(),
            pos.actorUserId(),
            command.drawId(),
            command.drawChannelId(),
            now,
            lineContexts
        );
    }

    private LimitLineContext toLimitLineContext(SellTicketLineInput line) {
        return new LimitLineContext(
            line.betType(),
            line.rawSelection(),
            toCents(line.stakeAmount()),
            0L
        );
    }

    private LimitLineContext toLimitLineContext(TicketLine line) {
        return new LimitLineContext(
            line.betType(),
            line.selection().key().value(),
            toCents(line.stakeAmount().amount()),
            toCents(line.potentialPayoutAmount().amount())
        );
    }

    private static long toCents(BigDecimal amount) {
        if (amount == null) {
            throw ProblemRest.badRequest("sales.invalid_stake_amount");
        }
        return amount.movePointRight(2).longValueExact();
    }
}
