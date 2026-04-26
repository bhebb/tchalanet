package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogAdminPort;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogAdminPort.PermissionSummary;
import com.tchalanet.server.core.accesscontrol.application.port.out.RolePermissionAdminPort;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PermissionCatalogAdminAdapter
    implements PermissionCatalogAdminPort, RolePermissionAdminPort {

  private final PermissionJpaRepository permissionRepository;
  private final RolePermissionAdminJpaRepository rolePermissionRepository;
  private final AppRoleJpaRepository appRoleRepository;

  @Override
  @Transactional(readOnly = true)
  public List<PermissionSummary> listPermissions() {
    return permissionRepository.findAllNotDeleted().stream().map(this::toSummary).toList();
  }

  private PermissionSummary toSummary(PermissionEntity entity) {
    return new PermissionSummary(
        entity.getCode(), entity.getName(), entity.getCategory(), entity.getDescription());
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = "role-permissions", key = "#roleId")
  public Set<String> listPermissionCodes(RoleId roleId) {
    return rolePermissionRepository.findByRoleId(roleId.value()).stream()
        .map(rp -> rp.getPermission().getCode())
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getRolePermissions(RoleId roleId) {
    // Alias vers listPermissionCodes pour compatibilité avec PermissionCatalogAdminPort
    return listPermissionCodes(roleId);
  }

  @Override
  @CacheEvict(cacheNames = "role-permissions", key = "#roleId")
  public boolean grant(RoleId roleId, String permissionCode) {
    if (roleId == null || permissionCode == null || permissionCode.isBlank()) {
      throw new IllegalArgumentException("roleId and permissionCode must not be null/blank");
    }

    var existing =
        rolePermissionRepository.findByRoleId(roleId.value()).stream()
            .anyMatch(rp -> rp.getPermission().getCode().equals(permissionCode));
    if (existing) {
      return false; // idempotent: lien déjà présent
    }

    var permission =
        permissionRepository
            .findById(permissionCode)
            .orElseThrow(
                () -> new IllegalArgumentException("Permission not found: " + permissionCode));

    var role =
        appRoleRepository
            .findById(roleId.value())
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

    var link = new AppRolePermissionEntity();
    link.setId(new AppRolePermissionId(roleId.value(), permissionCode));
    link.setRole(role);
    link.setPermission(permission);

    rolePermissionRepository.save(link);
    log.info("Granted permission {} to role {}", permissionCode, roleId);
    return true;
  }

  @Override
  @CacheEvict(cacheNames = "role-permissions", key = "#roleId")
  public boolean revoke(RoleId roleId, String permissionCode) {
    if (roleId == null || permissionCode == null || permissionCode.isBlank()) {
      throw new IllegalArgumentException("roleId and permissionCode must not be null/blank");
    }

    var links = new HashSet<>(rolePermissionRepository.findByRoleId(roleId.value()));
    var toRemove =
        links.stream().filter(rp -> rp.getPermission().getCode().equals(permissionCode)).toList();

    if (toRemove.isEmpty()) {
      return false; // rien à révoquer
    }

    rolePermissionRepository.deleteAll(toRemove);
    log.info("Revoked permission {} from role {}", permissionCode, roleId);
    return true;
  }
}
