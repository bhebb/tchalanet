package com.tchalanet.server.core.limitpolicy.domain.ports.in;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Inbound Port for evaluating all relevant limit policies for a ticket creation request. */
public interface EvaluateLimitsForTicketUseCase {

  LimitEvaluationResult evaluate(LimitEvaluationCommand command);

  record LimitEvaluationCommand(
      UUID tenantId, UUID terminalId, UUID userId, UUID sessionId, List<TicketLineInfo> lines) {}

  record TicketLineInfo(String gameCode, String selection, BigDecimal stake) {}
}
