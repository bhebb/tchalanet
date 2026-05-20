package com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.platform.accesscontrol.internal.service.Permission;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.RolePermissionJpaRepository;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** JPA reader for role-permission mappings from app_role, permission and role_permission. */
@Component
public class RolePermissionRepositoryJpaAdapter {

  private final RolePermissionJpaRepository rolePermissionRepository;

  public RolePermissionRepositoryJpaAdapter(RolePermissionJpaRepository rolePermissionRepository) {
    this.rolePermissionRepository = rolePermissionRepository;
  }

  public Set<Permission> getPermissionsForRoles(Set<TchRole> roles) {
    if (roles == null || roles.isEmpty()) {
      return Collections.emptySet();
    }

    // Map BusinessRole enum names to app_role.code values (they match by convention)
    Set<String> roleCodes = roles.stream().map(TchRole::name).collect(Collectors.toSet());

    var mappings = rolePermissionRepository.findByRoleCodes(roleCodes);
    if (mappings.isEmpty()) {
      return Collections.emptySet();
    }

    return mappings.stream()
        .map(m -> m.getPermission().getCode())
        .filter(code -> code != null && !code.isBlank())
        .map(Permission::new)
        .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
  }
}

