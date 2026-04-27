package com.tchalanet.server.core.autonomy.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.infra.persistence.mapper.AutonomyPolicyRuleMapper;
import com.tchalanet.server.core.autonomy.infra.persistence.repository.AutonomyPolicyRuleJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutonomyPolicyRuleRepositoryAdapter implements AutonomyPolicyRuleRepositoryPort {

  private final AutonomyPolicyRuleJpaRepository jpaRepository;
  private final AutonomyPolicyRuleMapper mapper;
  private final TchContextResolver tchContextResolver;

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

  @Override
  public AutonomyPolicyRule save(AutonomyPolicyRule policy) {
    var entity = mapper.toEntity(policy);
    // Populate tenant_id from request context (RLS-first). Adapter is infra layer so may set infra fields.
    try {
      var tenantUuid = tchContextResolver.currentOrThrow().tenantUuid();
      entity.setTenantId(tenantUuid);
    } catch (Exception ex) {
      // In case context is not available, fail-fast to avoid saving without tenant
      throw new IllegalStateException("Unable to determine current tenant for autonomy policy save", ex);
    }
    var saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
  }
}
