package com.tchalanet.server.accesscontrol.application;

import com.tchalanet.server.accesscontrol.application.port.in.RoleAdminUseCase;
import com.tchalanet.server.accesscontrol.infra.persistence.AppRoleEntity;
import com.tchalanet.server.accesscontrol.infra.persistence.AppRoleJpaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Impl des use cases admin pour la gestion des rôles.
 *
 * <p>NOTE : la sécurité d'accès à ces opérations est gérée au niveau web
 * via @RequiresPermission("roles.manage") + aspect accesscontrol.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RoleAdminService implements RoleAdminUseCase {

  private final AppRoleJpaRepository appRoleRepository;

  @Override
  public UUID upsertRole(
      UUID roleId,
      String code,
      String name,
      String description,
      UUID tenantId,
      UUID parentRoleId,
      boolean system) {
    AppRoleEntity entity;

    if (roleId != null) {
      entity =
          appRoleRepository
              .findById(roleId)
              .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    } else {
      entity = new AppRoleEntity();
      entity.setId(UUID.randomUUID());
    }

    entity.setCode(code);
    entity.setName(name);
    entity.setDescription(description);
    entity.setTenantId(tenantId);
    entity.setParentRoleId(parentRoleId);
    entity.setSystem(system);

    appRoleRepository.save(entity);
    return entity.getId();
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoleSummary> listRoles(UUID tenantId) {
    var entities =
        (tenantId == null)
            ? appRoleRepository.findAllGlobalNotDeleted()
            : appRoleRepository.findAllForTenantOrGlobal(tenantId);

    return entities.stream()
        .map(
            e ->
                new RoleSummary(
                    e.getId(),
                    e.getCode(),
                    e.getName(),
                    e.getDescription(),
                    e.getTenantId(),
                    e.getParentRoleId(),
                    e.isSystem()))
        .toList();
  }
}
