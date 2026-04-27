package com.tchalanet.server.core.subscription.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for tenant_subscription table (core/subscription).
 * Maps to spec requirement S7 (tenant-scoped persistence & RLS).
 *
 * CRITICAL CHANGES from catalog/billing (per REFACTORING_GUIDE.md):
 * - Table name: subscription → tenant_subscription
 * - plan_id (UUID FK) → plan_code (String, soft reference, no FK)
 * - Removed: billingProvider, billingExternalId (out of scope, separate module)
 * - Added: started_at, trial_ends_at, canceled_at
 * - metadata_json (extensibility)
 */
@Entity
@Table(name = "tenant_subscription")
@Getter
@Setter
@Audited
public class SubscriptionJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "plan_code", nullable = false, length = 128)
  private String planCode; // ✅ soft reference (string), validated at runtime

  @Column(name = "status", nullable = false, length = 50)
  private String status; // TRIAL|ACTIVE|SUSPENDED|CANCELED|EXPIRED

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "ends_at")
  private Instant endsAt;

  @Column(name = "trial_ends_at")
  private Instant trialEndsAt;

  @Column(name = "canceled_at")
  private Instant canceledAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", columnDefinition = "jsonb")
  private Map<String, String> metadataJson;
}
