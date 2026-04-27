package com.tchalanet.server.core.tenantconfig.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.common.types.enums.TenantType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for TenantConfig.
 * Table: tenant (no RLS, platform registry)
 * Per DOMAIN_TENANT_CONFIG.md:
 * - Platform-wide tenant registry (no RLS needed)
 * - Unique constraint on code
 * - Optional references to address and theme
 * Per typed_ids.md: UUID in entity, typed IDs in domain
 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantJpaEntity extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 64)
  private String code;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "type", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private TenantType type;

  @Column(name = "timezone", nullable = false, length = 64)
  private String timezone;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "status", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private TenantStatus status;

  @Column(name = "address_id", columnDefinition = "UUID")
  private UUID addressId;

  @Column(name = "active_theme_id", columnDefinition = "UUID")
  private UUID activeThemeId;
}
