package com.tchalanet.server.core.sales.internal.application.service.sell.model;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import com.tchalanet.server.platform.identity.api.model.AutonomyLevel;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

// TODO sales-sell: split PreparedSale into grouped records when agent/commission/offline/tax
// preparation starts adding more fields.
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
    PromotionDecision promotionDecision,
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
