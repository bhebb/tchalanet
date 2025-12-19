package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.core.accesscontrol.application.port.out.RolePermissionReaderPort;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RolePermissionReaderAdapter implements RolePermissionReaderPort {

  private final PermissionHierarchyJpaRepository permissionHierarchyRepository;

  @Override
  @Cacheable(cacheNames = "role-permissions", key = "#roleId")
  public Set<String> findPermissionCodesForRoleHierarchy(UUID roleId) {
    if (roleId == null) {
      return Set.of();
    }
    List<String> codes = permissionHierarchyRepository.findPermissionCodesForRoleHierarchy(roleId);
    return new HashSet<>(codes);
  }

  @Override
  public boolean roleHierarchyHasPermission(UUID roleId, String permissionCode) {
    if (roleId == null || permissionCode == null || permissionCode.isBlank()) {
      return false;
    }
    return findPermissionCodesForRoleHierarchy(roleId).contains(permissionCode);
  }
}
