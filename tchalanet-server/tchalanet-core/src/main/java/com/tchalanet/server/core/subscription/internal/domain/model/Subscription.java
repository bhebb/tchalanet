package com.tchalanet.server.core.subscription.internal.domain.model;

import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain model for tenant-scoped subscription (rich domain model).
 */
public record Subscription(
    SubscriptionId id,
    TenantId tenantId,
    String planCode,
    SubscriptionStatus status,
    Instant startedAt,
    Instant endsAt,
    Instant trialEndsAt,
    Instant canceledAt,
    Map<String, String> metadata,
    long version,
    Instant createdAt,
    Instant updatedAt,
    String createdBy
) {
  public Subscription {
    if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
    if (planCode == null || planCode.isBlank()) {
      throw new IllegalArgumentException("planCode is required");
    }
    if (status == null) throw new IllegalArgumentException("status is required");
  }

  /**
   * Cancel subscription immediately.
   * Sets status to CANCELED and canceled_at to now.
   */
  public Subscription cancelNow(Instant now) {
    if (status == SubscriptionStatus.CANCELED) {
      throw new IllegalStateException("Subscription already canceled: " + id);
    }
    Instant at = now != null ? now : Instant.now();
    return new Subscription(
        id,
        tenantId,
        planCode,
        SubscriptionStatus.CANCELED,
        startedAt,
        at, // ends_at = canceled_at
        trialEndsAt,
        at, // canceled_at
        metadata,
        version,
        createdAt,
        Instant.now(),
        createdBy
    );
  }

  /**
   * Resume subscription (from SUSPENDED to ACTIVE).
   */
  public Subscription resume(Instant now) {
    if (status != SubscriptionStatus.SUSPENDED) {
      throw new IllegalStateException("Cannot resume subscription with status: " + status);
    }
    return withStatus(SubscriptionStatus.ACTIVE);
  }

  /**
   * Suspend subscription (from ACTIVE to SUSPENDED).
   */
  public Subscription suspend(Instant now) {
    if (status != SubscriptionStatus.ACTIVE) {
      throw new IllegalStateException("Cannot suspend subscription with status: " + status);
    }
    return withStatus(SubscriptionStatus.SUSPENDED);
  }

  /**
   * Change plan (immediate change, no prorating for MVP).
   * @param newPlanCode the new plan value (validated by caller via PlanCatalog)
   */
  public Subscription changePlan(String newPlanCode, Instant now) {
    if (newPlanCode == null || newPlanCode.isBlank()) {
      throw new IllegalArgumentException("newPlanCode is required");
    }
    return new Subscription(
        id,
        tenantId,
        newPlanCode, // ✅ change plan value
        status,
        startedAt,
        endsAt,
        trialEndsAt,
        canceledAt,
        metadata,
        version,
        createdAt,
        Instant.now(),
        createdBy
    );
  }

  /**
   * Renew subscription (extend ends_at).
   */
  public Subscription renew(Instant newEndsAt) {
    if (newEndsAt == null) {
      throw new IllegalArgumentException("newEndsAt is required");
    }
    return new Subscription(
        id,
        tenantId,
        planCode,
        status,
        startedAt,
        newEndsAt, // ✅ extend period
        trialEndsAt,
        canceledAt,
        metadata,
        version,
        createdAt,
        Instant.now(),
        createdBy
    );
  }

  /**
   * Update metadata (generic).
   */
  public Subscription withMetadata(Map<String, String> newMetadata) {
    return new Subscription(
        id,
        tenantId,
        planCode,
        status,
        startedAt,
        endsAt,
        trialEndsAt,
        canceledAt,
        newMetadata != null ? newMetadata : new HashMap<>(),
        version,
        createdAt,
        Instant.now(),
        createdBy
    );
  }

  /**
   * Change status (generic helper).
   */
  public Subscription withStatus(SubscriptionStatus newStatus) {
    if (newStatus == null) {
      throw new IllegalArgumentException("newStatus is required");
    }
    return new Subscription(
        id,
        tenantId,
        planCode,
        newStatus,
        startedAt,
        endsAt,
        trialEndsAt,
        canceledAt,
        metadata,
        version,
        createdAt,
        Instant.now(),
        createdBy
    );
  }
}
