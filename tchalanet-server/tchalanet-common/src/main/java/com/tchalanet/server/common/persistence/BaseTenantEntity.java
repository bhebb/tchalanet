package com.tchalanet.server.common.persistence;

import com.tchalanet.server.common.persistence.audit.TenantEntityListener;
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

  @Column(name = "tenant_id", nullable = false, updatable = false, columnDefinition = "uuid")
  private UUID tenantId;
}
