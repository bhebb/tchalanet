package com.tchalanet.server.core.autonomy.internal.infra.persistence;

import com.tchalanet.server.core.autonomy.api.AutonomyTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AutonomyPolicyRuleJpaRepository extends JpaRepository<AutonomyPolicyRuleJpaEntity, UUID> {

    /**
     * Overview / Ops read:
     * - DO NOT filter deletedAt
     * - RLS decides visibility (active/deleted/all) via app.deleted_visibility
     */
    @Query("""
        SELECT e
        FROM AutonomyPolicyRuleJpaEntity e
        WHERE e.targetType = :targetType
          AND e.targetId = :targetId
        """)
    Optional<AutonomyPolicyRuleJpaEntity> findByTarget(
        AutonomyTargetType targetType, UUID targetId);

    /**
     * Runtime-safe read (used by resolver / enforcement):
     * - Always ignore deleted rules
     */
    @Query("""
        SELECT e
        FROM AutonomyPolicyRuleJpaEntity e
        WHERE e.targetType = :targetType
          AND e.targetId = :targetId
          AND e.enabled = true
          AND (e.startsAt IS NULL OR e.startsAt <= :now)
          AND (e.endsAt IS NULL OR e.endsAt > :now)
          AND e.deletedAt IS NULL
        """)
    Optional<AutonomyPolicyRuleJpaEntity> findActiveRuntime(
        AutonomyTargetType targetType, UUID targetId, Instant now);

    /**
     * Upsert-safe read:
     * - Update only an active row (deleted row should not be resurrected implicitly)
     * - If only a deleted row exists, handler will create a new row (works with partial unique index).
     */
    @Query("""
        SELECT e
        FROM AutonomyPolicyRuleJpaEntity e
        WHERE e.targetType = :targetType
          AND e.targetId = :targetId
          AND e.deletedAt IS NULL
        """)
    Optional<AutonomyPolicyRuleJpaEntity> findByTargetActiveOnly(
        AutonomyTargetType targetType, UUID targetId);
}
