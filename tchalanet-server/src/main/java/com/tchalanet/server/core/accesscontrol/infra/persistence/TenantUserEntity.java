package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "tenant_user")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class TenantUserEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false)
  private TenantId tenantId;

  @Column(name = "user_id", nullable = false)
  private String userId; // Keycloak sub, string

  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  @Column(name = "autonomy_level", nullable = false)
  private String autonomyLevel;

  @Column(name = "is_owner", nullable = false)
  private Boolean owner = false;
}
