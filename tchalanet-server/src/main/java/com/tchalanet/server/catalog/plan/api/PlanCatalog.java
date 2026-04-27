package com.tchalanet.server.catalog.plan.api;

import com.tchalanet.server.common.types.id.PlanId;

import java.util.List;
import java.util.Optional;

/**
 * Public API for Plan Catalog (read-only).
 * Maps to spec requirement P1 (read operations).
 *
 * This is the ONLY interface that core/subscription should depend on.
 * No access to internal persistence or JPA entities allowed.
 */
public interface PlanCatalog {

  /**
   * List all active plans (deleted_at IS NULL AND active=true).
   * Cacheable.
   * Maps to spec P1 scenario "listActive filters soft-deleted and inactive".
   */
  List<PlanView> listActive();

  /**
   * Find plan by code (functional key).
   * Returns plan even if active=false (but filters deleted_at IS NULL).
   * Cacheable.
   * Maps to spec P1 scenario "findByCode returns inactive plan".
   */
  Optional<PlanView> findByCode(String code);

  /**
   * Find plan by ID (technical key).
   * Filters deleted_at IS NULL.
   * Cacheable.
   * Maps to spec P1 (findById).
   */
  Optional<PlanView> findById(PlanId id);
}
