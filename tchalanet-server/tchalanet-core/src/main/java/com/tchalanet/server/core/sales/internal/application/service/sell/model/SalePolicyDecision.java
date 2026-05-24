package com.tchalanet.server.core.sales.internal.application.service.sell.model;

import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.platform.identity.api.model.AutonomyLevel;

import java.util.Objects;

public record SalePolicyDecision(
    LimitEvaluationView limits,
    AutonomyLevel autonomy,
    boolean requiresApproval,
    AutonomyLevel approvalLevel
) {

    public SalePolicyDecision {
        Objects.requireNonNull(limits, "limits is required");

        if (requiresApproval && approvalLevel == null) {
            throw new IllegalArgumentException("approvalLevel is required when approval is required");
        }

        if (!requiresApproval && approvalLevel != null) {
            throw new IllegalArgumentException("approvalLevel must be null when approval is not required");
        }
    }

    public static SalePolicyDecision allowed(LimitEvaluationView limits) {
        return new SalePolicyDecision(limits, null, false, null);
    }

    public static SalePolicyDecision allowedWithWarning(LimitEvaluationView limits) {
        return new SalePolicyDecision(limits, null, false, null);
    }

    public static SalePolicyDecision requiresApproval(LimitEvaluationView limits, AutonomyLevel level) {
        return new SalePolicyDecision(limits, level, true, level);
    }
}
