package com.tchalanet.server.core.limitpolicy.infra.persistence.repository;

import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LimitAssignmentJpaRepository
    extends JpaRepository<LimitAssignmentJpaEntity, UUID> {

  List<LimitAssignmentJpaEntity> findByTenantIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
      UUID tenantId, String targetType, UUID targetId);

  boolean existsByTenantIdAndLimitDefinitionIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
      UUID tenantId, UUID limitDefinitionId, String targetType, UUID targetId);

  List<LimitAssignmentJpaEntity> findActiveByTenantId(UUID uuid);
}
