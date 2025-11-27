package com.tchalanet.server.accesscontrol.infra.adapter;

import com.tchalanet.server.accesscontrol.infra.persistence.PermissionHierarchyJpaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionCatalogJpaAdapter
    implements com.tchalanet.server.accesscontrol.application.port.out.PermissionCatalogPort {

  private final PermissionHierarchyJpaRepository hierarchyRepository;

  @Override
  @Cacheable(cacheNames = "role-permissions", key = "#roleId")
  public Set<String> findPermissionsForRoleHierarchy(UUID roleId) {
    List<String> codes = hierarchyRepository.findPermissionCodesForRoleHierarchy(roleId);
    return new HashSet<>(codes);
  }
}
