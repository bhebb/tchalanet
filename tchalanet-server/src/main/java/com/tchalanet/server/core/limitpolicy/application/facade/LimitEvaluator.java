package com.tchalanet.server.core.limitpolicy.application.facade;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import com.tchalanet.server.core.limitpolicy.domain.model.OperationType;

public interface LimitEvaluator {
  LimitEvaluationResult evaluate(OperationType operationType, LimitContext context);
}
