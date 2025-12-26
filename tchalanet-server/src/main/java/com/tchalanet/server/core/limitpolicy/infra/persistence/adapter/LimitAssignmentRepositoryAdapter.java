package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentWriterPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitAssignmentMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitAssignmentJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitAssignmentRepositoryAdapter
    implements LimitAssignmentReaderPort, LimitAssignmentWriterPort {

  private final LimitAssignmentJpaRepository jpaRepository;
  private final LimitAssignmentMapper mapper;

  @Override
  public List<LimitAssignment> findActiveByTarget(
      TenantId tenantId, String targetType, UUID targetId) {
    return jpaRepository
        .findByTenantIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
            tenantId.uuid(), targetType, targetId)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<LimitAssignment> findById(TenantId tenantId, UUID assignmentId) {
    return jpaRepository
        .findById(assignmentId)
        .filter(e -> e.getTenantId().equals(tenantId.uuid()) && e.getDeletedAt() == null)
        .map(mapper::toDomain);
  }

  @Override
  public boolean existsByTenantAndLimitAndTarget(
      TenantId tenantId, UUID limitDefinitionId, String targetType, UUID targetId) {
    return jpaRepository
        .existsByTenantIdAndLimitDefinitionIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
            tenantId.uuid(), limitDefinitionId, targetType, targetId);
  }

  @Override
  public LimitAssignment save(LimitAssignment assignment) {
    var entity = mapper.toEntity(assignment);
    entity = jpaRepository.save(entity);
    return mapper.toDomain(entity);
  }

  @Override
  public void softDelete(TenantId tenantId, UUID assignmentId) {
    var entity = jpaRepository.findById(assignmentId).orElseThrow();
    if (!entity.getTenantId().equals(tenantId.uuid())) {
      throw new IllegalArgumentException("Assignment not found");
    }
    entity.setDeletedAt(Instant.now());
    jpaRepository.save(entity);
  }
}
