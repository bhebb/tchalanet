package com.tchalanet.server.core.autonomy.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.AutonomyPolicyRuleId;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyRuleWriterPort;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.infra.persistence.AutonomyPolicyRuleJpaRepository;
import com.tchalanet.server.core.autonomy.infra.persistence.AutonomyPolicyRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AutonomyPolicyRuleWriterAdapter implements AutonomyRuleWriterPort {

    private final AutonomyPolicyRuleJpaRepository jpaRepository;
    private final AutonomyPolicyRuleMapper mapper;
    private final TchContextResolver tchContextResolver;


    @Override
    public AutonomyPolicyRule save(AutonomyPolicyRule policy) {
        var entity = mapper.toEntity(policy);
        // Populate tenant_id from request context (RLS-first). Adapter is infra layer so may set infra fields.
        var tenantUuid = tchContextResolver.currentOrThrow().tenantUuid();
        entity.setTenantId(tenantUuid);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void softDelete(AutonomyPolicyRuleId id, Instant deletedAt) {
        var entity = jpaRepository.findById(id.value())
            .orElseThrow(() -> new IllegalArgumentException("autonomy rule not found"));

        entity.setDeletedAt(deletedAt);
        jpaRepository.save(entity);
    }
}
