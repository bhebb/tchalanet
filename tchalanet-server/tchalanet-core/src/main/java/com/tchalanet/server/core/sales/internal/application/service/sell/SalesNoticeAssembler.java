package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.web.advice.ApiResponseBodyAdvice;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SalePolicyDecision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link ApiNotice} entries from policy decisions made during sale preparation.
 *
 * <p>Code conventions:
 * <ul>
 *   <li>{@code sales.limit.<rule>} for limit-related notices.</li>
 *   <li>{@code sales.charge.<type>} for charge disclosures.</li>
 *   <li>{@link ApiResponseBodyAdvice#APPROVAL_REQUIRED_CODE} for the special
 *       APPROVAL_REQUIRED notice that drives {@code ApiStatus.PENDING}.</li>
 * </ul>
 */
public final class SalesNoticeAssembler {

    private static final String DOMAIN = "sales";

    private SalesNoticeAssembler() {
    }


    public static ArrayList<ApiNotice> assemble(SalePolicyDecision policyDecision, List<TicketCharge> charges, PromotionDecision promotionDecision) {
        var notices = new ArrayList<ApiNotice>();
        notices.addAll(SalesNoticeAssembler.fromLimits(policyDecision.limits()));
        notices.addAll(SalesNoticeAssembler.fromCharges(charges));
        if (promotionDecision != null) {
            notices.addAll(SalesNoticeAssembler.fromPromotionDecision(promotionDecision));
        }
        if (policyDecision.requiresApproval()) {
            notices.add(SalesNoticeAssembler.approvalRequired(
                policyDecision.approvalLevel().name()
            ));
        }
        return notices;
    }
    // -------------------------------------------------------------------------
    // Limit notices
    // -------------------------------------------------------------------------

    public static List<ApiNotice> fromLimits(LimitEvaluationView limits) {
        if (limits == null || limits.breaches() == null || limits.breaches().isEmpty()) {
            return List.of();
        }
        var notices = new ArrayList<ApiNotice>(limits.breaches().size());
        for (var breach : limits.breaches()) {
            var severity = mapSeverity(breach.outcome());
            if (severity == null) continue;
            Map<String, Object> meta = new HashMap<>();
            if (breach.appliedScope() != null) meta.put("scope", breach.appliedScope());
            if (breach.currentValue() != null) meta.put("currentValue", breach.currentValue());
            if (breach.limitValue() != null) meta.put("limitValue", breach.limitValue());
            String code = breach.ruleKey() == null
                ? "sales.limit_breach"
                : "sales.limit." + breach.ruleKey().name().toLowerCase();
            notices.add(new ApiNotice(
                code,
                breach.messageKey(),
                DOMAIN,
                severity,
                Map.copyOf(meta)
            ));
        }
        return List.copyOf(notices);
    }

    private static NoticeSeverity mapSeverity(BreachOutcome outcome) {
        if (outcome == null) return null;
        return switch (outcome) {
            case ALLOW -> null;
            case WARN, REQUIRE_APPROVAL -> NoticeSeverity.WARN;
            case BLOCK -> NoticeSeverity.WARN; // BLOCK throws upstream; shouldn't reach here
        };
    }

    // -------------------------------------------------------------------------
    // Charge disclosures
    // -------------------------------------------------------------------------

    public static List<ApiNotice> fromCharges(List<TicketCharge> charges) {
        if (charges == null || charges.isEmpty()) return List.of();
        var notices = new ArrayList<ApiNotice>(charges.size());
        for (var charge : charges) {
            notices.add(new ApiNotice(
                "sales.charge." + charge.type().name().toLowerCase(),
                "sales.notice.charge_applied",
                DOMAIN,
                NoticeSeverity.INFO,
                Map.of(
                    "chargeType", charge.type().name(),
                    "amount", charge.amount().amount(),
                    "currency", charge.amount().currency().code(),
                    "paidBy", charge.paidBy().name()
                )
            ));
        }
        return List.copyOf(notices);
    }

    public static List<ApiNotice> fromPromotionDecision(PromotionDecision promotionDecision) {
        if (promotionDecision == null || promotionDecision.notices() == null || promotionDecision.notices().isEmpty()) {
            return List.of();
        }

        // Promotion API uses List<String> notices. Map each string to a simple ApiNotice.
        return promotionDecision.notices().stream()
            .map(noticeStr -> new ApiNotice(
                noticeStr,
                noticeStr,
                "promotionDecision",
                NoticeSeverity.INFO,
                Map.of()
            ))
            .toList();
    }

    // -------------------------------------------------------------------------
    // APPROVAL_REQUIRED
    // -------------------------------------------------------------------------

    public static ApiNotice approvalRequired(String approvalLevel) {
        return new ApiNotice(
            ApiResponseBodyAdvice.APPROVAL_REQUIRED_CODE,
            "sales.notice.approval_required",
            DOMAIN,
            NoticeSeverity.WARN,
            Map.of("approvalLevel", approvalLevel)
        );
    }
}
