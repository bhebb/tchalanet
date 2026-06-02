package com.tchalanet.server.platform.tenant.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Tenant/outlet business-day open/close override (holidays, exceptional closures).
 *
 * <p>{@code outlet_id IS NULL} = tenant-level rule; non-null = outlet-level
 * (wins over tenant). Tenant-scoped via RLS; {@code tenant_id} is set explicitly
 * by the write service from the request context (this entity extends
 * {@link BaseEntity}, not {@code BaseTenantEntity}, because there is no
 * {@code business_day_override_aud} table — it is not Envers-audited).
 */
@Entity
@Table(name = "business_day_override")
@Getter
@Setter
public class BusinessDayOverrideJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false, updatable = false, columnDefinition = "uuid")
  private UUID tenantId;

  /** Null = tenant-level override; set = outlet-level override. */
  @Column(name = "outlet_id", updatable = false, columnDefinition = "uuid")
  private UUID outletId;

  @Column(name = "business_date", nullable = false, updatable = false)
  private LocalDate businessDate;

  /** false = closed that day, true = explicitly open (override a recurring closed-weekday). */
  @Column(name = "open", nullable = false)
  private boolean open;

  @Column(name = "reason_code", length = 96)
  private String reasonCode;

  @Column(name = "label", length = 255)
  private String label;
}
