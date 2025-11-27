package com.tchalanet.server.accesscontrol.infra.persistence;

import com.tchalanet.server.common.infra.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "tenant_user",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id"}))
@Audited
@Getter
@Setter
@NoArgsConstructor
public class TenantUserJpaEntity extends BaseTenantEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  // référence vers app_role.id (UUID). On conserve la FK en tant que UUID pour simplicité.
  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  @Column(name = "autonomy_level", nullable = false, length = 16)
  private String autonomyLevel = "none";

  @Column(name = "is_owner", nullable = false)
  private boolean owner = false;
}
