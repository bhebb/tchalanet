package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.domain.model.TargetType;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitDefinitionJpaEntity;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitDefinitionMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitAssignmentJpaRepository;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitDefinitionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitDefinitionRepositoryAdapter implements LimitDefinitionReaderPort {

  private final LimitDefinitionJpaRepository defRepo;
  private final LimitAssignmentJpaRepository assignRepo;
  private final LimitDefinitionMapper mapper;

  @Override
  public List<LimitDefinition> findActiveByTenantId(UUID tenantId) {
    return defRepo.findActiveByTenantId(tenantId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<LimitAssignment> findActiveAssignmentsByTenantId(UUID tenantId) {
    return assignRepo.findActiveByTenantId(tenantId).stream()
        .map(mapper::toAssignmentDomain)
        .toList();
  }

  @Override
  public Optional<LimitDefinition> findById(UUID definitionId) {
    return defRepo.findById(definitionId)
        .filter(e -> e.getDeletedAt() == null)
        .map(mapper::toDomain);
  }

  @Override
  public List<LimitAssignment> findActiveAssignmentsByTenantAndTarget(UUID tenantId, TargetType targetType, UUID targetId) {
    return assignRepo.findByTenantIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(tenantId, targetType.name(), targetId).stream()
        .map(mapper::toAssignmentDomain)
        .toList();
  }
}
