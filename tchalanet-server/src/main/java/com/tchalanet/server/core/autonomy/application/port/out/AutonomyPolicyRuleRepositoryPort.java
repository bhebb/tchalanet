package com.tchalanet.server.core.autonomy.application.port.out;

import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRuleRule;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AutonomyPolicyRuleRuleRepositoryPort {
    Optional<AutonomyPolicyRuleRule> findActive(UUID tenantId, AutonomyTargetType targetType, UUID targetId, Instant now);

    Optional<AutonomyPolicyRuleRule> findByTarget(UUID tenantId, AutonomyTargetType targetType, UUID targetId);

    AutonomyPolicyRuleRule save(AutonomyPolicyRuleRule policy);
}
