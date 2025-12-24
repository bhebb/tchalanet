package com.tchalanet.server.core.autonomy.infra.persistence.repository;

import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.infra.persistence.entity.AutonomyPolicyRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AutonomyPolicyRuleJpaRepository extends JpaRepository<AutonomyPolicyRuleJpaEntity, UUID> {

    @Query("SELECT e FROM AutonomyPolicyRuleJpaEntity e WHERE e.tenantId = :tenantId AND e.targetType = :targetType AND e.targetId = :targetId AND e.deletedAt IS NULL")
    Optional<AutonomyPolicyRuleJpaEntity> findByTarget(UUID tenantId, AutonomyTargetType targetType, UUID targetId);

    @Query("SELECT e FROM AutonomyPolicyRuleJpaEntity e WHERE e.tenantId = :tenantId AND e.targetType = :targetType AND e.targetId = :targetId AND e.enabled = true AND (e.startsAt IS NULL OR e.startsAt <= :now) AND (e.endsAt IS NULL OR e.endsAt > :now) AND e.deletedAt IS NULL")
    Optional<AutonomyPolicyRuleJpaEntity> findActive(UUID tenantId, AutonomyTargetType targetType, UUID targetId, Instant now);
}
