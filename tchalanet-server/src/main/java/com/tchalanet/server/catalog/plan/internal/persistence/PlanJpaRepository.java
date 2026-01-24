package com.tchalanet.server.catalog.plan.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for billing_plan (catalog/plan).
 * All queries filter deleted_at IS NULL implicitly via entity state.
 */
@Repository
public interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {

  /**
   * Find plan by code (case-insensitive), filtering soft-deleted.
   * Maps to spec P1 (findByCode).
   */
  Optional<PlanJpaEntity> findFirstByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  /**
   * List all active plans (active=true, deleted_at IS NULL).
   * Maps to spec P1 (listActive).
   */
  List<PlanJpaEntity> findAllByActiveTrueAndDeletedAtIsNull();

  /**
   * Find by ID, filtering soft-deleted.
   * Maps to spec P1 (findById).
   */
  Optional<PlanJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
}
