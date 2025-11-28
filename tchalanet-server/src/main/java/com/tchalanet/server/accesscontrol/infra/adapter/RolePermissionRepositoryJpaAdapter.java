package com.tchalanet.server.accesscontrol.infra.adapter;

import com.tchalanet.server.accesscontrol.application.port.out.RolePermissionRepositoryPort;
import com.tchalanet.server.accesscontrol.domain.model.Permission;
import com.tchalanet.server.accesscontrol.domain.model.TchRole;
import com.tchalanet.server.accesscontrol.infra.persistence.RolePermissionJpaRepository;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * JPA-based implementation of {@link RolePermissionRepositoryPort} that reads role-permission
 * mappings from the database tables app_role, permission and role_permission.
 */
@Component
public class RolePermissionRepositoryJpaAdapter implements RolePermissionRepositoryPort {

  private final RolePermissionJpaRepository rolePermissionRepository;

  public RolePermissionRepositoryJpaAdapter(RolePermissionJpaRepository rolePermissionRepository) {
    this.rolePermissionRepository = rolePermissionRepository;
  }

  @Override
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
