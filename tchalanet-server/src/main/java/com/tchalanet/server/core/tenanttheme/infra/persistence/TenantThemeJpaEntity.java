package com.tchalanet.server.core.tenanttheme.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * JPA entity for tenant_theme table.
 * Maps to spec requirement T2.
 * RLS policies MUST be enforced at DB level.
 */
@Entity
@Table(name = "tenant_theme")
@Getter
@Setter
public class TenantThemeJpaEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private java.util.UUID tenantId;

    @Column(name = "preset_code", nullable = false, length = 128)
    private String presetCode;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, String> metadata;

  @Column(name = "is_default", nullable = false)
  private boolean defaultTheme = false;
}
