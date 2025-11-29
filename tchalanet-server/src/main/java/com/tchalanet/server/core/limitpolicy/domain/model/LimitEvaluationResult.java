package com.tchalanet.server.core.limitpolicy.domain.model;

import java.util.List;
import java.util.Objects;

/** Value Object representing the outcome of evaluating all relevant limit policies. */
public record LimitEvaluationResult(BreachOutcome overallOutcome, List<String> reasons) {
  public LimitEvaluationResult {
    Objects.requireNonNull(overallOutcome, "Overall outcome cannot be null");
    Objects.requireNonNull(reasons, "Reasons list cannot be null");
  }

  public static LimitEvaluationResult block(List<String> reasons) {
    return new LimitEvaluationResult(BreachOutcome.BLOCK, reasons);
  }

  public static LimitEvaluationResult warn(List<String> reasons) {
    return new LimitEvaluationResult(BreachOutcome.WARN, reasons);
  }

  public static LimitEvaluationResult allow() {
    return new LimitEvaluationResult(BreachOutcome.ALLOW, List.of());
  }
}
