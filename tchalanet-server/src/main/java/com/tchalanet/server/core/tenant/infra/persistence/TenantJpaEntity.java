package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.core.tenant.domain.model.Tenant;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import com.tchalanet.server.core.tenant.domain.model.TenantType;
import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "tenant")
@EntityListeners({TenantCacheEvictListener.class})
@Audited
@Getter
@Setter
@NoArgsConstructor
public class TenantJpaEntity extends BaseEntity {

  @Column(name = "code", nullable = false, length = 64, unique = true)
  private String code;

  @Column(name = "name", nullable = false, length = 160)
  private String name;

  @Column(name = "timezone", nullable = false, length = 64)
  private String timezone;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "status", nullable = false, length = 16)
  private String status; // ACTIVE|SUSPENDED|CLOSED

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private TenantType type;

  @Column(name = "active_theme_id")
  private UUID activeThemeId;

  @Column(name = "address_id")
  private UUID addressId;

  public TenantJpaEntity(Tenant t) {
    this.name = t.name();
    this.type = t.type();
    // other fields (code, timezone...) may be set by adapters
  }

  public Tenant toDomain() {
    TenantId tid = new TenantId(this.getId() != null ? this.getId() : null);
    return new Tenant(tid, name, type);
  }
}
