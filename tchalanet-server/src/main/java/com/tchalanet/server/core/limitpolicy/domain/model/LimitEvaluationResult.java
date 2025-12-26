package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import java.util.List;

/**
 * Result of evaluating limits against a transaction context.
 *
 * <p>Contains the overall outcome (ALLOW, WARN, BLOCK) and detailed information about any breaches
 * that occurred during evaluation.
 */
public record LimitEvaluationResult(BreachOutcome overallOutcome, List<LimitBreachDetail> details) {
  public static LimitEvaluationResult allow() {
    return new LimitEvaluationResult(BreachOutcome.ALLOW, List.of());
  }
}
