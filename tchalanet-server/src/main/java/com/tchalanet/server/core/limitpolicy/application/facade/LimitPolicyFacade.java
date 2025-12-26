package com.tchalanet.server.core.limitpolicy.application.facade;

import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Facade service for limit policy evaluation.
 *
 * Provides a simplified interface for evaluating betting limits against transaction contexts.
 * Delegates to the underlying LimitEvaluator for the actual evaluation logic.
 */
@Service
@RequiredArgsConstructor
public class LimitPolicyFacade {

  private final LimitEvaluator evaluator;

  public LimitEvaluationResult evaluate(OperationType operationType, LimitContext context) {
    // For now, delegate to evaluator
    return evaluator.evaluate(operationType, context);
  }
}
