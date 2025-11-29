package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@EntityListeners({TenantCacheEvictListener.class})
@Audited
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

  @Column(name = "active_theme_id")
  private String activeThemeId;
}
