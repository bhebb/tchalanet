package com.tchalanet.server.core.autonomy.infra.persistence.adapter;

import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.infra.persistence.mapper.AutonomyPolicyRuleMapper;
import com.tchalanet.server.core.autonomy.infra.persistence.repository.AutonomyPolicyRuleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AutonomyPolicyRuleRepositoryAdapter implements AutonomyPolicyRuleRepositoryPort {

    private final AutonomyPolicyRuleJpaRepository jpaRepository;
    private final AutonomyPolicyRuleMapper mapper;

    @Override
    public Optional<AutonomyPolicyRule> findActive(TenantId tenantId, AutonomyTargetType targetType, UUID targetId, Instant now) {
        return jpaRepository.findActive(tenantId.uuid(), targetType, targetId, now)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AutonomyPolicyRule> findByTarget(TenantId tenantId, AutonomyTargetType targetType, UUID targetId) {
        return jpaRepository.findByTarget(tenantId.uuid(), targetType, targetId)
                .map(mapper::toDomain);
    }

    @Override
    public AutonomyPolicyRule save(AutonomyPolicyRule policy) {
        var entity = mapper.toEntity(policy);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
