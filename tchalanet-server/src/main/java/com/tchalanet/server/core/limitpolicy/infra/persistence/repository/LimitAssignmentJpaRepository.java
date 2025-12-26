package com.tchalanet.server.core.limitpolicy.infra.persistence.repository;
import com.querydsl.jpa.QueryHandler;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LimitAssignmentJpaRepository extends JpaRepository<LimitAssignmentJpaEntity, UUID> {

    List<LimitAssignmentJpaEntity> findByTenantIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(UUID tenantId, String targetType, UUID targetId);

    boolean existsByTenantIdAndLimitDefinitionIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(UUID tenantId, UUID limitDefinitionId, String targetType, UUID targetId);

    List<LimitAssignmentJpaEntity> findActiveByTenantId(UUID uuid);
}
