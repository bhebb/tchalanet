package com.tchalanet.server.core.autonomy.application.port.out;

import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AutonomyPolicyRuleRepositoryPort {
  Optional<AutonomyPolicyRule> findActiveRuntime(AutonomyTargetType targetType, UUID targetId, Instant now);

  Optional<AutonomyPolicyRule> findByTarget(AutonomyTargetType targetType, UUID targetId);

  Optional<AutonomyPolicyRule> findByTargetActiveOnly(AutonomyTargetType targetType, UUID targetId);

  AutonomyPolicyRule save(AutonomyPolicyRule policy);
}
