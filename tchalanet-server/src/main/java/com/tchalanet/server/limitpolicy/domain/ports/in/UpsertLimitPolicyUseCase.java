package com.tchalanet.server.limitpolicy.domain.ports.in;

import com.tchalanet.server.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.limitpolicy.domain.model.LimitPolicy;
import com.tchalanet.server.limitpolicy.domain.model.LimitScope;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Inbound Port for creating or updating limit policies. */
public interface UpsertLimitPolicyUseCase {

  LimitPolicy upsert(UpsertLimitPolicyCommand command);

  Optional<LimitPolicy> getLimitPolicy(UUID policyId);

  record UpsertLimitPolicyCommand(
      UUID id, // Null for creation, UUID for update
      UUID tenantId,
      LimitScope scope,
      String target, // e.g., gameCode, terminalId, userId
      BigDecimal dailyCap,
      BigDecimal maxStakePerLine,
      BigDecimal maxPayoutPerLine,
      BreachOutcome onBreach,
      boolean active) {}
}
