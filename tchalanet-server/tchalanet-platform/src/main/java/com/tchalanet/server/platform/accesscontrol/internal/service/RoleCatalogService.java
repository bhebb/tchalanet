package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CreateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantPermissionToRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolesRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RevokePermissionFromRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.UpdateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RolePermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RoleView;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.PermissionCatalogAdminAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Role catalog and role-permission management.
 *
 * <p>In V1 these write operations are platform-ops only; tenant admins have read-only access.
 * Tenant custom roles are deferred to the 2027 roadmap.
 */
@Service
@RequiredArgsConstructor
public class RoleCatalogService {

  private final AppRoleJpaRepository appRoleRepository;
  private final PermissionCatalogAdminAdapter permissionCatalogAdminAdapter;

  public List<RoleView> listRoles(ListRolesRequest request) {
    List<AppRoleJpaEntity> roles =
        request.tenantId() == null
            ? appRoleRepository.findAllGlobalNotDeleted()
            : appRoleRepository.findAllForTenantOrGlobal(request.tenantId().value());
    return roles.stream().map(this::toRoleView).toList();
  }

  public List<RolePermissionView> listRolePermissions(ListRolePermissionsRequest request) {
    return permissionCatalogAdminAdapter.listPermissionCodes(request.roleId()).stream()
        .map(code -> new RolePermissionView(request.roleId(), code))
        .toList();
  }

  public RoleView createRole(CreateRoleRequest request) {
    var entity = new AppRoleJpaEntity();
    entity.setCode(request.code());
    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setTenantId(request.tenantId() == null ? null : request.tenantId().value());
    UUID roleUuid = appRoleRepository.save(entity).getId();
    return appRoleRepository.findById(roleUuid).map(this::toRoleView)
        .orElseGet(() -> new RoleView(RoleId.of(roleUuid), request.code(), request.name(),
            request.description(), request.tenantId(), null, request.system()));
  }

  public RoleView updateRole(UpdateRoleRequest request) {
    var entity = appRoleRepository.findById(request.id().value())
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.id()));
    entity.setCode(request.code());
    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setTenantId(request.tenantId() == null ? null : request.tenantId().value());
    UUID roleUuid = appRoleRepository.save(entity).getId();
    return appRoleRepository.findById(roleUuid).map(this::toRoleView)
        .orElseGet(() -> new RoleView(RoleId.of(roleUuid), request.code(), request.name(),
            request.description(), request.tenantId(), null, request.system()));
  }

  public void grantPermission(GrantPermissionToRoleRequest request) {
    permissionCatalogAdminAdapter.grant(request.roleId(), request.permissionCode());
  }

  public void revokePermission(RevokePermissionFromRoleRequest request) {
    permissionCatalogAdminAdapter.revoke(request.roleId(), request.permissionCode());
  }

  private RoleView toRoleView(AppRoleJpaEntity entity) {
    return new RoleView(
        RoleId.of(entity.getId()),
        entity.getCode(),
        entity.getName(),
        entity.getDescription(),
        TenantId.nullableOf(entity.getTenantId()),
        null,
        entity.getTenantId() == null);
  }
}
