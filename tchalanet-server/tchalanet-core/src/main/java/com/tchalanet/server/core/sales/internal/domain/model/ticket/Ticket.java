package com.tchalanet.server.core.sales.internal.domain.model.ticket;


import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.lifecycle.ApprovalTrace;
import com.tchalanet.server.core.sales.api.model.lifecycle.ResultLifecycle;
import com.tchalanet.server.core.sales.api.model.lifecycle.SaleLifecycle;
import com.tchalanet.server.core.sales.api.model.lifecycle.SettlementLifecycle;
import com.tchalanet.server.core.sales.api.model.lifecycle.TicketLifecycle;
import com.tchalanet.server.core.sales.api.model.line.TicketLineResult;
import com.tchalanet.server.core.sales.api.model.money.TicketMoney;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.api.model.origin.TicketOrigin;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintState;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ticket — Aggregate root of the sales domain.
 *
 * <p>An immutable record carrying the full state of a sales ticket across its three lifecycles:
 * sale (PENDING_APPROVAL → APPROVED/REJECTED/CANCELLED/VOIDED),
 * result (NOT_RESULTED → WON/LOST/VOID/OVERRIDDEN),
 * settlement (NOT_SETTLED → PAYOUT_PENDING/PAID/NO_PAYOUT).
 *
 * <p>Invariants enforced at construction:
 * <ul>
 *   <li>Sum of line {@code potentialPayoutAmount} equals ticket {@code potentialPayoutAmount}.
 *   <li>All lines share the ticket currency.
 *   <li>Lines are stored as an immutable copy.
 * </ul>
 *
 * <p>All mutations return a new {@code Ticket} instance with an incremented version.
 */
public record Ticket(
    TicketIdentity identity,
    TicketContext context,
    TicketCodes codes,
    TicketMoney money,
    TicketLifecycle lifecycle,
    TicketOrigin origin,
    TicketPrintState print,
    TicketAudit audit,
    List<TicketLine> lines
) {

    public Ticket {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Ticket must have at least one line");
        }

        // Currency consistency
        var currency = money.currency();
        for (TicketLine line : lines) {
            if (!line.stakeAmount().currency().equals(currency)) {
                throw new IllegalArgumentException(
                    "Line " + line.id() + " currency does not match ticket currency");
            }
        }

        // Invariant: ticket.potentialPayout == Σ lines.potentialPayout
        var sumOfLines = lines.stream()
            .map(TicketLine::potentialPayoutAmount)
            .reduce(Money.zero(currency), Money::plus);
        if (!money.potentialPayoutAmount().equals(sumOfLines)) {
            throw new IllegalArgumentException(
                "Ticket potentialPayout (" + money.potentialPayoutAmount()
                    + ") must equal sum of line potentialPayouts (" + sumOfLines + ")");
        }

        lines = List.copyOf(lines);
    }

    // ===========================================================================
    // Factory
    // ===========================================================================

    /**
     * Place a new ticket in its initial state.
     *
     * @param requiresApproval decided upstream by LimitPolicy (e.g. buyer limit exceeded).
     *                         Only {@code POS_ONLINE} / {@code WEB} / {@code PARTNER_API}
     *                         channels accept {@code true}; other channels reject it
     *                         because the sales decision has already been taken upstream.
     */
    public static Ticket place(TicketIdentity id, TicketContext context, TicketCodes codes,
                               TicketMoneyBreakdown breakdown, List<TicketLine> lines,
                               TicketSaleChannel channel, boolean requiresApproval,
                               ApprovalRequestId approvalRequestId,
                               UserId actorUserId, Instant now) {
        if (requiresApproval && !channel.allowsPendingApproval()) {
            throw new IllegalArgumentException(
                "Approval workflow not allowed for channel=" + channel
                    + ". Decision must be taken upstream.");
        }

        var currency = breakdown.total().currency();
        var potentialPayout = lines.stream()
            .map(TicketLine::potentialPayoutAmount)
            .reduce(Money.zero(currency), Money::plus);

        var money = new TicketMoney(currency, breakdown, potentialPayout);

        var initialStatus = requiresApproval
            ? TicketSaleStatus.PENDING_APPROVAL
            : TicketSaleStatus.APPROVED;
        var sales = SaleLifecycle.initial(initialStatus, now);
        if (requiresApproval) {
            sales = sales.withApprovalRequest(approvalRequestId, actorUserId, now);
        }
        return new Ticket(
            id,
            context,
            codes,
            money,
            new TicketLifecycle(
                sales,
                ResultLifecycle.notResulted(currency),
                SettlementLifecycle.notSettled()
            ),
            new TicketOrigin(channel),
            TicketPrintState.notPrinted(),
            TicketAudit.created(actorUserId, now),
            lines
        );
    }

    // ===========================================================================
    // Sale lifecycle
    // ===========================================================================

    public Ticket requestApproval(
        ApprovalRequestId requestId, UserId requestedBy, Instant now
    ) {
        requireSaleStatus(TicketSaleStatus.PENDING_APPROVAL);
        return withSale(lifecycle.sale().withApprovalRequest(requestId, requestedBy, now))
            .touchedBy(requestedBy, now);
    }

    /**
     * Approve a ticket previously in {@code PENDING_APPROVAL} state.
     *
     * @param approvedBy the user approving the ticket
     * @param reason     optional reason for the approval (may be null)
     * @param now        the approval timestamp
     * @return new Ticket instance in APPROVED state
     * @throws IllegalStateException if the ticket is not in PENDING_APPROVAL state
     */
    public Ticket approve(UserId approvedBy, String reason, Instant now) {
        requireSaleStatus(TicketSaleStatus.PENDING_APPROVAL);
        requireApprovalTrace();
        requireOptionalReason(reason);
        return withSale(lifecycle.sale().approved(approvedBy, now))
            .touchedBy(approvedBy, now);
    }

    public Ticket reject(UserId rejectedBy, String reason, Instant now) {
        requireSaleStatus(TicketSaleStatus.PENDING_APPROVAL);
        requireReason(reason);
        return withSale(lifecycle.sale().rejected(rejectedBy, reason, now))
            .touchedBy(rejectedBy, now);
    }

    public Ticket cancel(UserId cancelledBy, String reason, Instant now) {
        if (!lifecycle.sale().status().isCancellable()) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " in status " + lifecycle.sale().status()
                    + " cannot be cancelled");
        }
        if (lifecycle.result().status() != TicketResultStatus.NOT_RESULTED) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " is already resulted, cannot cancel");
        }
        requireReason(reason);
        return withSale(lifecycle.sale().cancelled(cancelledBy, reason, now))
            .touchedBy(cancelledBy, now);
    }

    public Ticket voidTicket(UserId voidedBy, String reason, Instant now) {
        requireReason(reason);
        return withSale(lifecycle.sale().voided(voidedBy, reason, now))
            .touchedBy(voidedBy, now);
    }

    // ===========================================================================
    // Result lifecycle
    // ===========================================================================

    /**
     * Apply the official result (from draw) to this ticket.
     * Aggregates the line-level results into the ticket-level result.
     */
    public Ticket applyOfficialResult(
        Map<TicketLineId, TicketLineResult> lineResults,
        UserId actorUserId,
        Instant now
    ) {
        requireSaleStatus(TicketSaleStatus.APPROVED);
        if (lifecycle.result().status() != TicketResultStatus.NOT_RESULTED) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " is already resulted");
        }
        return applyResultInternal(lineResults, actorUserId, null, now, false);
    }

    /**
     * Override a previously-applied result. Requires a non-empty reason.
     * Final status becomes {@code OVERRIDDEN}, regardless of the new computed status.
     */
    public Ticket overrideResult(
        Map<TicketLineId, TicketLineResult> lineResults,
        UserId actorUserId,
        String reason,
        Instant now
    ) {
        if (lifecycle.result().status() == TicketResultStatus.NOT_RESULTED) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " is not yet resulted, cannot override");
        }
        requireReason(reason);
        return applyResultInternal(lineResults, actorUserId, reason, now, true);
    }

    private Ticket applyResultInternal(
        Map<TicketLineId, TicketLineResult> lineResults,
        UserId actorUserId,
        String overrideReason,
        Instant now,
        boolean override
    ) {
        List<TicketLine> updatedLines = lines.stream()
            .map(line -> {
                TicketLineResult result = lineResults.get(line.id());
                if (result == null) {
                    throw new IllegalArgumentException(
                        "Missing result for line " + line.id());
                }
                return line.withResult(result);
            })
            .toList();

        Money winning = updatedLines.stream()
            .map(TicketLine::payoutAmount)
            .reduce(Money.zero(money.currency()), Money::plus);

        TicketResultStatus newStatus = override
            ? TicketResultStatus.OVERRIDDEN
            : computeAggregateResultStatus(updatedLines);

        ResultLifecycle newResult = override
            ? lifecycle.result().overridden(newStatus, winning, actorUserId, overrideReason, now)
            : lifecycle.result().resulted(newStatus, winning, actorUserId, now);

        return new Ticket(
            identity.bumpVersion(),
            context, codes, money,
            new TicketLifecycle(lifecycle.sale(), newResult, lifecycle.settlement()),
            origin, print,
            audit.updated(actorUserId, now),
            updatedLines
        );
    }

    private static TicketResultStatus computeAggregateResultStatus(List<TicketLine> lines) {
        boolean anyWon = lines.stream().anyMatch(l -> l.resultStatus() == TicketLineResultStatus.WON);
        if (anyWon) return TicketResultStatus.WON;
        boolean allVoid = lines.stream().allMatch(l -> l.resultStatus() == TicketLineResultStatus.VOID);
        if (allVoid) return TicketResultStatus.VOID;
        return TicketResultStatus.LOST;
    }

    // ===========================================================================
    // Settlement lifecycle
    // ===========================================================================

    public Ticket settle(UserId settledBy, Instant now) {
        if (lifecycle.result().status() == TicketResultStatus.NOT_RESULTED) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " is not yet resulted, cannot settle");
        }
        if (lifecycle.settlement().status() != TicketSettlementStatus.NOT_SETTLED) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " is already settled");
        }
        SettlementLifecycle newSettlement = lifecycle.result().winningAmount().isZero()
            ? lifecycle.settlement().settledWithoutPayout(settledBy, now)
            : lifecycle.settlement().settledPendingPayout(settledBy, now);
        return withSettlement(newSettlement).touchedBy(settledBy, now);
    }

    public Ticket markPaid(UserId paidBy, Instant paidAt) {
        if (lifecycle.settlement().status() != TicketSettlementStatus.PAYOUT_PENDING) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " is not in PAYOUT_PENDING state, cannot mark paid");
        }
        return withSettlement(lifecycle.settlement().paid(paidBy, paidAt))
            .touchedBy(paidBy, paidAt);
    }

    public Ticket markPayoutReversed(UserId reversedBy, Instant reversedAt) {
        if (lifecycle.settlement().status() != TicketSettlementStatus.PAID) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " is not in PAID state, cannot reverse payout");
        }
        return withSettlement(lifecycle.settlement().reversed(reversedBy, reversedAt))
            .touchedBy(reversedBy, reversedAt);
    }

    // ===========================================================================
    // Print
    // ===========================================================================

    public Ticket markPrinted(UserId printedBy, Instant now) {
        if (!lifecycle.sale().status().isPrintable()) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " in status " + lifecycle.sale().status()
                    + " cannot be printed"
            );
        }
        return new Ticket(
            identity.bumpVersion(),
            context, codes, money, lifecycle, origin,
            print.markPrinted(now),
            audit.updated(printedBy, now),
            lines
        );
    }

    // ===========================================================================
    // Derived reads
    // ===========================================================================

    public Money potentialPayout() {
        return money.potentialPayoutAmount();
    }

    public Money winningAmount() {
        return lifecycle.result().winningAmount();
    }

    public Optional<ApprovalRequestId> approvalRequestId() {
        return Optional.ofNullable(lifecycle.sale().approval())
            .map(ApprovalTrace::requestId);
    }

    // ===========================================================================
    // Internal helpers
    // ===========================================================================

    private void requireSaleStatus(TicketSaleStatus expected) {
        if (lifecycle.sale().status() != expected) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " expected sale status " + expected
                    + " but was " + lifecycle.sale().status());
        }
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }
    }

    private static void requireOptionalReason(String reason) {
        if (reason != null && reason.isBlank()) {
            throw new IllegalArgumentException("Reason must not be blank");
        }
    }

    private void requireApprovalTrace() {
        var approval = lifecycle.sale().approval();
        if (approval == null || approval.requestId() == null) {
            throw new IllegalStateException(
                "Ticket " + identity.id() + " is pending approval without approval trace");
        }
    }

    private Ticket withSale(SaleLifecycle sale) {
        return new Ticket(
            identity.bumpVersion(),
            context, codes, money,
            new TicketLifecycle(sale, lifecycle.result(), lifecycle.settlement()),
            origin, print, audit, lines
        );
    }

    private Ticket withSettlement(SettlementLifecycle settlement) {
        return new Ticket(
            identity.bumpVersion(),
            context, codes, money,
            new TicketLifecycle(lifecycle.sale(), lifecycle.result(), settlement),
            origin, print, audit, lines
        );
    }

    private Ticket touchedBy(UserId actor, Instant now) {
        return new Ticket(
            identity, context, codes, money, lifecycle, origin, print,
            audit.updated(actor, now),
            lines
        );
    }
}
