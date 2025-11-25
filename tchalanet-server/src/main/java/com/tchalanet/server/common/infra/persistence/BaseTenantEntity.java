package com.tchalanet.server.common.infra.persistence;

import com.tchalanet.server.common.infra.persistence.audit.TenantEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EntityListeners({TenantEntityListener.class})
@Getter
@Setter
public abstract class BaseTenantEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;
}
