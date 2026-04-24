package com.tchalanet.server.core.limitpolicy.infra.persistence.repository;

import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitAssignmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitAssignmentJpaRepository extends JpaRepository<LimitAssignmentJpaEntity, UUID> {

    List<LimitAssignmentJpaEntity> findAllByTargetTypeAndTargetId(TargetType targetType, UUID targetId);

    /**
     * For TENANT target: targetId is NULL in DB.
     */
    List<LimitAssignmentJpaEntity> findAllByTargetTypeAndTargetIdIsNull(TargetType targetType);

    Optional<LimitAssignmentJpaEntity> findByLimitDefinitionIdAndTargetTypeAndTargetId(
        UUID limitDefinitionId, TargetType targetType, UUID targetId);

    Optional<LimitAssignmentJpaEntity> findByLimitDefinitionIdAndTargetTypeAndTargetIdIsNull(
        UUID limitDefinitionId, TargetType targetType);

    void deleteByLimitDefinitionId(UUID limitDefinitionId);
}
