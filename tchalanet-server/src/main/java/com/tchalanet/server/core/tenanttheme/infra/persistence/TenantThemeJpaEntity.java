package com.tchalanet.server.core.tenanttheme.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.common.types.id.TenantId;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

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

  @Type(JsonType.class)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, String> metadata;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "created_by", length = 255)
  private String createdBy;
}
