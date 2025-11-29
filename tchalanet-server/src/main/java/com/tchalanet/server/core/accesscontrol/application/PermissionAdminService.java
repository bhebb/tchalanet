package com.tchalanet.server.core.accesscontrol.application;

import com.tchalanet.server.core.accesscontrol.application.port.in.PermissionAdminUseCase;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleJpaRepository;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRolePermissionEntity;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRolePermissionId;
import com.tchalanet.server.core.accesscontrol.infra.persistence.PermissionEntity;
import com.tchalanet.server.core.accesscontrol.infra.persistence.PermissionJpaRepository;
import com.tchalanet.server.core.accesscontrol.infra.persistence.RolePermissionAdminJpaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Impl des use cases admin pour la gestion des permissions et du mapping rôle -> permissions. */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PermissionAdminService implements PermissionAdminUseCase {

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
  public Set<String> getRolePermissions(UUID roleId) {
    return rolePermissionRepository.findByRoleId(roleId).stream()
        .map(rp -> rp.getPermission().getCode())
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  @Override
  @CacheEvict(cacheNames = "role-permissions", allEntries = true)
  public void setRolePermissions(UUID roleId, Set<String> permissionCodes) {
    log.info("Setting permissions for role {}: {}", roleId, permissionCodes);

    // Defensive checks
    if (roleId == null) {
      log.warn("setRolePermissions called with null roleId");
      throw new IllegalArgumentException("roleId must not be null");
    }

    // Supprimer les anciennes associations
    rolePermissionRepository.deleteByRoleId(roleId);

    if (permissionCodes == null || permissionCodes.isEmpty()) {
      log.debug("No permission codes provided for role {} - cleared existing permissions", roleId);
      return;
    }

    // Charger les permissions existantes pour éviter les FK errors
    var existing = new HashSet<>(permissionRepository.findAllByCodeIn(permissionCodes));
    var existingCodes =
        existing.stream()
            .map(PermissionEntity::getCode)
            .collect(java.util.stream.Collectors.toSet());

    if (existingCodes.isEmpty()) {
      log.warn(
          "None of the provided permission codes exist in catalog for role {}: {}",
          roleId,
          permissionCodes);
      return;
    }

    // Load role entity reference
    var roleEntity =
        appRoleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

    // Créer les liens rôle ↔ permission
    var links =
        existing.stream()
            .map(
                pe -> {
                  AppRolePermissionEntity link = new AppRolePermissionEntity();
                  link.setId(new AppRolePermissionId(roleId, pe.getCode()));
                  link.setRole(roleEntity);
                  link.setPermission(pe);
                  return link;
                })
            .toList();

    rolePermissionRepository.saveAll(links);
    log.info("Saved {} permission links for role {}", links.size(), roleId);
  }
}
