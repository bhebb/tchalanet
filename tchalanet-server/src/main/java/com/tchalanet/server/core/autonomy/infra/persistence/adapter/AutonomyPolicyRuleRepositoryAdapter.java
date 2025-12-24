package com.tchalanet.server.core.autonomy.infra.persistence.adapter;

import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRuleRule;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.infra.persistence.entity.AutonomyPolicyRuleJpaEntity;
import com.tchalanet.server.core.autonomy.infra.persistence.mapper.AutonomyPolicyRuleMapper;
import com.tchalanet.server.core.autonomy.infra.persistence.repository.AutonomyPolicyRuleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AutonomyPolicyRuleRuleRepositoryAdapter implements AutonomyPolicyRuleRuleRepositoryPort {

    private final AutonomyPolicyRuleJpaRepository jpaRepository;
    private final AutonomyPolicyRuleMapper mapper;

    @Override
    public Optional<AutonomyPolicyRuleRule> findActive(UUID tenantId, AutonomyTargetType targetType, UUID targetId, Instant now) {
        return jpaRepository.findActive(tenantId, targetType, targetId, now)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AutonomyPolicyRuleRule> findByTarget(UUID tenantId, AutonomyTargetType targetType, UUID targetId) {
        return jpaRepository.findByTarget(tenantId, targetType, targetId)
                .map(mapper::toDomain);
    }

    @Override
    public AutonomyPolicyRuleRule save(AutonomyPolicyRuleRule policy) {
        var entity = mapper.toEntity(policy);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
