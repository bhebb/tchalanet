package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * OUTLET-LEVEL business-day override, owned by core.outlet.
 *
 * <p>Maps to the shared {@code business_day_override} table but always with
 * {@code outlet_id NOT NULL}. Tenant-level rows (outlet_id NULL) are owned by
 * {@code platform.tenantconfig} — this entity never touches them.
 *
 * <p>Not Envers-audited (no {@code business_day_override_aud} table); attribution
 * via {@code created_by}/{@code updated_by} on {@link BaseEntity}. Tenant
 * isolation is enforced by RLS.
 */
@Entity(name = "OutletBusinessDayOverride")
@Table(name = "business_day_override")
@Getter
@Setter
public class OutletBusinessDayOverrideJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false, updatable = false, columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "outlet_id", nullable = false, updatable = false, columnDefinition = "uuid")
  private UUID outletId;

  @Column(name = "business_date", nullable = false, updatable = false)
  private LocalDate businessDate;

  @Column(name = "open", nullable = false)
  private boolean open;

  @Column(name = "reason_code", length = 96)
  private String reasonCode;

  @Column(name = "label", length = 255)
  private String label;
}
