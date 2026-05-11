package com.tchalanet.server.core.autonomy.infra.persistence.adapter;

import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyRuleReaderPort;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.infra.persistence.AutonomyPolicyRuleJpaRepository;
import com.tchalanet.server.core.autonomy.infra.persistence.AutonomyPolicyRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AutonomyPolicyRuleReaderAdapter implements AutonomyRuleReaderPort {

    private final AutonomyPolicyRuleJpaRepository jpaRepository;
    private final AutonomyPolicyRuleMapper mapper;

    @Override
    public Optional<AutonomyPolicyRule> findEffective(
        TenantId tenantId,
        OutletId outletId,
        UserId userId,
        Instant now
    ) {
        if (userId != null) {
            var found = findActiveRuntime(AutonomyTargetType.USER, userId.value(), now);
            if (found.isPresent()) return found;
        }

        if (outletId != null) {
            var found = findActiveRuntime(AutonomyTargetType.OUTLET, outletId.value(), now);
            if (found.isPresent()) return found;
        }

        return findActiveRuntime(AutonomyTargetType.TENANT, tenantId.value(), now);
    }

    @Override
    public Optional<AutonomyPolicyRule> findActiveRuntime(AutonomyTargetType targetType, UUID targetId, Instant now) {
        // JPA repository relies on RLS; do not pass tenantId to JPA methods
        return jpaRepository.findActiveRuntime(targetType, targetId, now).map(mapper::toDomain);
    }

    @Override
    public Optional<AutonomyPolicyRule> findByTarget(AutonomyTargetType targetType, UUID targetId) {
        return jpaRepository.findByTarget(targetType, targetId).map(mapper::toDomain);
    }

    @Override
    public Optional<AutonomyPolicyRule> findByTargetActiveOnly(AutonomyTargetType targetType, UUID targetId) {
        return jpaRepository.findByTargetActiveOnly(targetType, targetId).map(mapper::toDomain);
    }
}
