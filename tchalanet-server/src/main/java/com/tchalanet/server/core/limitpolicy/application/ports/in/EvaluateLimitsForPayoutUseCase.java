package com.tchalanet.server.core.limitpolicy.application.ports.in;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import java.math.BigDecimal;
import java.util.UUID;

public interface EvaluateLimitsForPayoutUseCase {
  LimitEvaluationResult evaluate(PayoutLimitEvaluationCommand command);

  record PayoutLimitEvaluationCommand(UUID tenantId, UUID ticketId, UUID outletId, UUID cashierId, BigDecimal amount) {}
}

