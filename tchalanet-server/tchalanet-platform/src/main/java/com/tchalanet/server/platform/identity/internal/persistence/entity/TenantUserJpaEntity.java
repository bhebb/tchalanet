package com.tchalanet.server.platform.identity.internal.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.platform.identity.api.model.TenantUserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "tenant_user",
    uniqueConstraints =
        @UniqueConstraint(
            name = "ux_tenant_user_tenant_user", columnNames = {"tenant_id", "user_id"}))
@Audited
@Getter
@Setter
public class TenantUserJpaEntity extends BaseTenantEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 32)
  private TenantUserStatus status;

  @Column(name = "is_owner")
  private Boolean isOwner = Boolean.FALSE;

  @Column(name = "outlet_id")
  private UUID outletId;

  @Column(name = "terminal_id")
  private UUID terminalId;
}
