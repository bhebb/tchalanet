package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitPolicy;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScope;
import com.tchalanet.server.core.limitpolicy.application.ports.out.LimitPolicyRepositoryPort;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitPolicyEntity;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitPolicyMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.SpringLimitPolicyJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaLimitPolicyRepositoryAdapter implements LimitPolicyRepositoryPort {

  private final SpringLimitPolicyJpaRepository jpaRepository;
  private final LimitPolicyMapper mapper;

  @Override
  public LimitPolicy save(LimitPolicy policy) {
    LimitPolicyEntity entity = mapper.toEntity(policy);
    LimitPolicyEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<LimitPolicy> findById(UUID policyId) {
    return jpaRepository.findById(policyId).map(mapper::toDomain);
  }

  @Override
  public List<LimitPolicy> findActivePolicies(UUID tenantId) {
    return jpaRepository.findByTenantIdAndActiveTrue(tenantId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<LimitPolicy> findActivePoliciesByScopeAndTarget(
      UUID tenantId, LimitScope scope, String target) {
    return jpaRepository
        .findByTenantIdAndScopeAndTargetAndActiveTrue(tenantId, scope, target)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}
