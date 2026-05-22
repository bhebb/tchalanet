package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.core.autonomy.api.query.ResolveAutonomyQuery;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import com.tchalanet.server.core.limitpolicy.api.query.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitLineContext;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.internal.application.rule.DrawCutoffRule;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import com.tchalanet.server.core.session.api.query.PosOperationAction;
import com.tchalanet.server.core.session.api.query.ResolvePosOperationContextQuery;
import com.tchalanet.server.platform.communication.api.CommunicationFeePolicy;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.identity.api.model.AutonomyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Prepares a ticket sale: validates the command, resolves the POS frame,
 * computes ticket lines and charges, evaluates limit policy and seller
 * autonomy, and aggregates user-facing notices.
 *
 * <p>This service makes ALL pre-commit decisions. The handler only consumes
 * {@link PreparedSale} and persists. No business logic lives in the handler.
 */
@Component
@RequiredArgsConstructor
public class TicketSalePolicyService {

    private final DrawCutoffRule drawCutoffRule;
    private final QueryBus queryBus;
    private final TicketLinePreparationService ticketLinePreparationService;
    private final SelectionApi selectionApi;
    private final CommunicationFeePolicy communicationFeePolicy;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public PreparedSale prepareSale(SellTicketCommand command, TchRequestContext ctx) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(ctx, "ctx is required");

        validateCommand(command);

        var now = Instant.now(clock);
        var tenantId = ctx.effectiveTenantIdRequired();

        var pos = resolvePosContext(ctx);
        var draw = drawCutoffRule.requireBeforeCutoff(command.drawId());

        // V1: no normalize/merge; restore canonical merge here later.
        var mergedLines = command.lines();
        var ticketLines = ticketLinePreparationService.toTicketLines(
            tenantId, mergedLines, command.currency()
        );

        var charges = computeCharges(tenantId, command);
        var moneyBreakdown = computeMoneyBreakdown(ticketLines, charges, command);

        var policyDecision = evaluateLimitsAndAutonomy(
            tenantId, command, pos, draw, mergedLines, now
        );

        // Aggregate notices from every source.
        var notices = new ArrayList<ApiNotice>();
        notices.addAll(SalesNoticeFactory.fromLimits(policyDecision.limits()));
        notices.addAll(SalesNoticeFactory.fromCharges(charges));
        if (policyDecision.requiresApproval()) {
            notices.add(SalesNoticeFactory.approvalRequired(
                policyDecision.approvalLevel().name()
            ));
        }

        ApprovalRequestId approvalRequestId = policyDecision.requiresApproval()
            ? ApprovalRequestId.of(idGenerator.newUuid())
            : null;

        return new PreparedSale(
            pos, draw, now,
            mergedLines, ticketLines, charges, moneyBreakdown,
            policyDecision.limits(),
            policyDecision.autonomy(),
            policyDecision.requiresApproval(),
            policyDecision.approvalLevel(),
            approvalRequestId,
            List.copyOf(notices)
        );
    }

    // -------------------------------------------------------------------------
    // POS
    // -------------------------------------------------------------------------

    private ValidatedPosOperationContext resolvePosContext(TchRequestContext ctx) {
        return queryBus.ask(new ResolvePosOperationContextQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.userId(),
            ctx.operationalContextRequired(),
            PosOperationAction.SELL_TICKET_ONLINE
        ));
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private void validateCommand(SellTicketCommand command) {
        if (command.drawId() == null) {
            throw ProblemRest.badRequest("sales.draw_required");
        }
        if (command.drawChannelId() == null) {
            throw ProblemRest.badRequest("sales.draw_channel_required");
        }
        if (command.currency() == null) {
            throw ProblemRest.badRequest("sales.currency_required");
        }
        if (command.lines() == null || command.lines().isEmpty()) {
            throw ProblemRest.badRequest("sales.lines_required");
        }
        var distinctLineNumbers = command.lines().stream()
            .map(SellTicketLineInput::lineNumber)
            .distinct().count();
        if (distinctLineNumbers != command.lines().size()) {
            throw ProblemRest.badRequest("sales.duplicate_line_number");
        }
        for (var line : command.lines()) {
            validateLine(line);
        }
    }

    private void validateLine(SellTicketLineInput line) {
        if (line.lineNumber() <= 0) throw ProblemRest.badRequest("sales.invalid_line_number");
        if (line.gameCode() == null) throw ProblemRest.badRequest("sales.game_required");
        if (line.betType() == null) throw ProblemRest.badRequest("sales.bet_type_required");
        if (!line.gameCode().supports(line.betType()))
            throw ProblemRest.badRequest("sales.unsupported_bet_type");
        if (line.rawSelection() == null || line.rawSelection().isBlank())
            throw ProblemRest.badRequest("sales.selection_required");
        if (line.stakeAmount() == null || line.stakeAmount().signum() <= 0)
            throw ProblemRest.badRequest("sales.invalid_stake_amount");
        validateBetOption(line);
        validateSelection(line);
    }

    private void validateSelection(SellTicketLineInput line) {
        try {
            selectionApi.canonicalize(line.betType(), line.betOption(), line.rawSelection());
        } catch (IllegalArgumentException ex) {
            throw ProblemRest.badRequest("sales.selection_invalid");
        }
    }

    private void validateBetOption(SellTicketLineInput line) {
        try {
            BetOption.from(line.betType(), line.betOption());
        } catch (IllegalArgumentException ex) {
            if (line.betType().requiresOption() && line.betOption() == null) {
                throw ProblemRest.badRequest("sales.bet_option_required");
            }
            if (!line.betType().requiresOption() && line.betOption() != null) {
                throw ProblemRest.badRequest("sales.bet_option_not_allowed");
            }
            throw ProblemRest.badRequest("sales.bet_option_out_of_range");
        }
    }

    // -------------------------------------------------------------------------
    // Charges (notification fees)
    // -------------------------------------------------------------------------

    private List<TicketCharge> computeCharges(TenantId tenantId, SellTicketCommand command) {
        var opts = command.communicationOptions();
        if (opts == null) {
            return List.of();
        }

        var charges = new ArrayList<TicketCharge>();

        if (opts.sendSms()) {
            appendCommunicationChargeIfApplicable(
                charges,
                tenantId,
                CommunicationChannel.SMS,
                TicketChargeType.BUYER_SMS
            );
        }

        if (opts.sendWhatsapp()) {
            appendCommunicationChargeIfApplicable(
                charges,
                tenantId,
                CommunicationChannel.WHATSAPP,
                TicketChargeType.BUYER_WHATSAPP
            );
        }

        if (opts.sendEmail()) {
            appendCommunicationChargeIfApplicable(
                charges,
                tenantId,
                CommunicationChannel.EMAIL,
                TicketChargeType.BUYER_EMAIL
            );
        }

        return List.copyOf(charges);
    }

    private void appendCommunicationChargeIfApplicable(
        List<TicketCharge> out,
        TenantId tenantId,
        CommunicationChannel channel,
        TicketChargeType chargeType
    ) {
        communicationFeePolicy.feeFor(tenantId, channel).ifPresent(fee -> {
            if (fee.amount().isZero()) {
                return;
            }

            out.add(new TicketCharge(
                chargeType,
                fee.amount(),
                ChargePaidBy.fromCommunicationFee(fee.paidBy())
            ));
        });
    }


    // -------------------------------------------------------------------------
    // Money breakdown
    // -------------------------------------------------------------------------

    private TicketMoneyBreakdown computeMoneyBreakdown(
        List<TicketLine> ticketLines,
        List<TicketCharge> charges,
        SellTicketCommand command
    ) {
        var zero = Money.zero(command.currency());
        var stake = ticketLines.stream()
            .map(TicketLine::stakeAmount)
            .reduce(zero, Money::plus);
        var buyerCharges = charges.stream()
            .filter(c -> c.paidBy() == ChargePaidBy.BUYER)
            .map(TicketCharge::amount)
            .reduce(zero, Money::plus);
        var total = stake.plus(buyerCharges);
        return new TicketMoneyBreakdown(stake, charges, total);
    }

    // -------------------------------------------------------------------------
    // Limit policy + autonomy
    // -------------------------------------------------------------------------

    private PolicyDecision evaluateLimitsAndAutonomy(
        TenantId tenantId,
        SellTicketCommand command,
        ValidatedPosOperationContext pos,
        DrawSummary draw,
        List<SellTicketLineInput> mergedLines,
        Instant now
    ) {
        var limitContext = toLimitContext(tenantId, command, pos, draw, mergedLines, now);
        var limits = queryBus.ask(new EvaluateLimitPolicyQuery(limitContext));

        if (limits == null) {
            throw ProblemRest.conflict("sales.limit_evaluation_failed");
        }

        if (limits.outcome() == null) {
            return PolicyDecision.allowed(limits);
        }

        return switch (limits.outcome()) {
            case ALLOW -> PolicyDecision.allowed(limits);
            case WARN -> PolicyDecision.allowedWithWarning(limits);
            case REQUIRE_APPROVAL, BLOCK -> resolveWithAutonomy(tenantId, pos, limits);
        };
    }

    private PolicyDecision resolveWithAutonomy(
        TenantId tenantId, ValidatedPosOperationContext pos, LimitEvaluationView limits
    ) {
        var autonomy = queryBus.ask(new ResolveAutonomyQuery(
            tenantId, pos.outletId(), pos.actorUserId(), limits.outcome()
        ));
        if (autonomy == null || autonomy.autonomyLevel() == null) {
            throw ProblemRest.conflict("sales.autonomy_resolution_failed");
        }
        if (autonomy.autonomyLevel() == AutonomyLevel.NONE) {
            throw ProblemRest.conflict("sales.limit_blocked");
        }
        return PolicyDecision.requiresApproval(limits, autonomy.autonomyLevel());
    }

    private LimitContext toLimitContext(
        TenantId tenantId,
        SellTicketCommand command,
        ValidatedPosOperationContext pos,
        DrawSummary draw,
        List<SellTicketLineInput> mergedLines,
        Instant now
    ) {
        var lineContexts = mergedLines.stream().map(this::toLimitLineContext).toList();
        return new LimitContext(
            tenantId, pos.outletId(), pos.actorUserId(),
            command.drawId(), command.drawChannelId(), now, lineContexts
        );
    }

    private LimitLineContext toLimitLineContext(SellTicketLineInput line) {
        return new LimitLineContext(
            line.betType(), line.rawSelection(), toCents(line.stakeAmount()), 0L
        );
    }

    private static long toCents(BigDecimal amount) {
        if (amount == null) throw ProblemRest.badRequest("sales.invalid_stake_amount");
        return amount.movePointRight(2).longValueExact();
    }

    // -------------------------------------------------------------------------
    // Output records
    // -------------------------------------------------------------------------

    public record PreparedSale(
        ValidatedPosOperationContext pos,
        DrawSummary draw,
        Instant now,
        List<SellTicketLineInput> mergedLines,
        List<TicketLine> ticketLines,
        List<TicketCharge> charges,
        TicketMoneyBreakdown moneyBreakdown,
        LimitEvaluationView limits,
        AutonomyLevel autonomyLevel,
        boolean requiresApproval,
        AutonomyLevel approvalLevel,
        ApprovalRequestId approvalRequestId,
        List<ApiNotice> notices
    ) {
        public PreparedSale {
            Objects.requireNonNull(pos);
            Objects.requireNonNull(draw);
            Objects.requireNonNull(now);
            Objects.requireNonNull(mergedLines);
            Objects.requireNonNull(ticketLines);
            Objects.requireNonNull(charges);
            Objects.requireNonNull(moneyBreakdown);
            Objects.requireNonNull(notices);
            if (requiresApproval != (approvalRequestId != null)) {
                throw new IllegalArgumentException(
                    "requiresApproval and approvalRequestId must be consistent");
            }
            charges = List.copyOf(charges);
            notices = List.copyOf(notices);
        }
    }

    private record PolicyDecision(
        LimitEvaluationView limits,
        AutonomyLevel autonomy,
        boolean requiresApproval,
        AutonomyLevel approvalLevel
    ) {
        static PolicyDecision allowed(LimitEvaluationView limits) {
            return new PolicyDecision(limits, null, false, null);
        }

        static PolicyDecision allowedWithWarning(LimitEvaluationView limits) {
            return new PolicyDecision(limits, null, false, null);
        }

        static PolicyDecision requiresApproval(LimitEvaluationView limits, AutonomyLevel level) {
            return new PolicyDecision(limits, level, true, level);
        }
    }
}
