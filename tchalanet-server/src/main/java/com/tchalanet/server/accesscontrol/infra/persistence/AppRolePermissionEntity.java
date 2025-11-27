package com.tchalanet.server.accesscontrol.infra.persistence;

import com.tchalanet.server.audit.infra.web.AuditLog;
import com.tchalanet.server.common.infra.persistence.AuditableEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppRolePermissionEntity extends AuditableEntity {

  @EmbeddedId private AppRolePermissionId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("roleId")
  @JoinColumn(name = "role_id", nullable = false)
  private AppRoleEntity role;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("permissionCode")
  @JoinColumn(name = "permission_code", referencedColumnName = "code", nullable = false)
  private PermissionEntity permission;

}
