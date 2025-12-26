package com.tchalanet.server.core.autonomy.application.port.out;

import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AutonomyPolicyRuleRepositoryPort {
  Optional<AutonomyPolicyRule> findActive(
      TenantId tenantId, AutonomyTargetType targetType, UUID targetId, Instant now);

  Optional<AutonomyPolicyRule> findByTarget(
      TenantId tenantId, AutonomyTargetType targetType, UUID targetId);

  AutonomyPolicyRule save(AutonomyPolicyRule policy);
}
