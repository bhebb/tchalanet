package com.tchalanet.server.core.sales.internal.application.service.sell.model;

import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.platform.identity.api.model.AutonomyLevel;
import java.util.Objects;

public record SalePolicyDecision(LimitEvaluationView limits, AutonomyLevel autonomy) {

  public SalePolicyDecision {
    Objects.requireNonNull(limits, "limits is required");
  }

  public static SalePolicyDecision allowed(LimitEvaluationView limits) {
    return new SalePolicyDecision(limits, null);
  }

  public static SalePolicyDecision allowedWithWarning(LimitEvaluationView limits) {
    return new SalePolicyDecision(limits, null);
  }
}
