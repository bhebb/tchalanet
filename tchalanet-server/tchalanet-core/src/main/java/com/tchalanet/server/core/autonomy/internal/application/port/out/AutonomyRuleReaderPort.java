package com.tchalanet.server.core.autonomy.internal.application.port.out;

import com.tchalanet.server.core.autonomy.api.AutonomyTargetType;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.autonomy.internal.domain.model.AutonomyPolicyRule;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AutonomyRuleReaderPort {
    Optional<AutonomyPolicyRule> findEffective(TenantId tenantId, OutletId outletId, UserId userId, Instant now);

    Optional<AutonomyPolicyRule> findActiveRuntime(AutonomyTargetType targetType, UUID targetId, Instant now);

    Optional<AutonomyPolicyRule> findByTarget(AutonomyTargetType targetType, UUID targetId);

    Optional<AutonomyPolicyRule> findByTargetActiveOnly(AutonomyTargetType targetType, UUID targetId);

}
