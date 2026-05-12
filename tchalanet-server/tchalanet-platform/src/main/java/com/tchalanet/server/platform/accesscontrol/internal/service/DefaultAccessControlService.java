package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.result.CheckUserPermissionsResult;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CreateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantPermissionToRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolesRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.view.PermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RevokePermissionFromRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RolePermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RoleView;
import com.tchalanet.server.platform.accesscontrol.api.model.request.SetTenantUserRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.UpdateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.PermissionCatalogAdminAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAccessControlService implements AccessControlApi {

  private final AppRoleJpaRepository appRoleRepository;
  private final PermissionCatalogAdminAdapter permissionCatalogAdminAdapter;

  @Override
  public CheckUserPermissionsResult checkPermissions(CheckUserPermissionsRequest request) {
    Set<String> required = request.requiredPermissions() == null ? Set.of() : request.requiredPermissions();
    if (required.isEmpty()) {
      return new CheckUserPermissionsResult(true, Set.of());
    }

    EffectivePermissionsView effective =
        getEffectivePermissions(new GetEffectivePermissionsRequest(request.userId(), request.tenantId()));

    Set<String> missing =
        required.stream().filter(permission -> !effective.permissionCodes().contains(permission)).collect(Collectors.toSet());

    return new CheckUserPermissionsResult(missing.isEmpty(), missing);
  }

  @Override
  public List<RoleView> listRoles(ListRolesRequest request) {
    List<AppRoleJpaEntity> roles =
        request.tenantId() == null
            ? appRoleRepository.findAllGlobalNotDeleted()
            : appRoleRepository.findAllForTenantOrGlobal(request.tenantId().value());

    return roles.stream().map(this::toRoleView).toList();
  }

  @Override
  public List<PermissionView> listPermissions(ListPermissionsRequest request) {
    return permissionCatalogAdminAdapter.listPermissions().stream()
        .map(summary -> new PermissionView(summary.code(), summary.name(), summary.category(), summary.description()))
        .toList();
  }

  @Override
  public List<RolePermissionView> listRolePermissions(ListRolePermissionsRequest request) {
    return permissionCatalogAdminAdapter.listPermissionCodes(request.roleId()).stream()
        .map(code -> new RolePermissionView(request.roleId(), code))
        .toList();
  }

  @Override
  public EffectivePermissionsView getEffectivePermissions(GetEffectivePermissionsRequest request) {
    return new EffectivePermissionsView(request.tenantId(), request.userId(), null, Set.of());
  }

  @Override
  public RoleView createRole(CreateRoleRequest request) {
    AppRoleJpaEntity entity = new AppRoleJpaEntity();
    entity.setCode(request.code());
    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setTenantId(request.tenantId() == null ? null : request.tenantId().value());
    entity.setParentRoleId(request.parentRoleId() == null ? null : request.parentRoleId().value());

    UUID roleUuid = appRoleRepository.save(entity).getId();

    return appRoleRepository.findById(roleUuid).map(this::toRoleView).orElseGet(() -> toRoleView(roleUuid, request));
  }

  @Override
  public RoleView updateRole(UpdateRoleRequest request) {
    AppRoleJpaEntity entity =
        appRoleRepository
            .findById(request.id().value())
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.id()));

    entity.setCode(request.code());
    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setTenantId(request.tenantId() == null ? null : request.tenantId().value());
    entity.setParentRoleId(request.parentRoleId() == null ? null : request.parentRoleId().value());

    UUID roleUuid = appRoleRepository.save(entity).getId();

    return appRoleRepository.findById(roleUuid).map(this::toRoleView).orElseGet(() -> toRoleView(roleUuid, request));
  }

  @Override
  public void grantPermission(GrantPermissionToRoleRequest request) {
    permissionCatalogAdminAdapter.grant(request.roleId(), request.permissionCode());
  }

  @Override
  public void revokePermission(RevokePermissionFromRoleRequest request) {
    permissionCatalogAdminAdapter.revoke(request.roleId(), request.permissionCode());
  }

  @Override
  public void setTenantUserRole(SetTenantUserRoleRequest request) {
    throw new UnsupportedOperationException(
        "setTenantUserRole requires identity writer wiring in platform.identity");
  }

  private RoleView toRoleView(AppRoleJpaEntity entity) {
    return new RoleView(
        RoleId.of(entity.getId()),
        entity.getCode(),
        entity.getName(),
        entity.getDescription(),
        TenantId.nullableOf(entity.getTenantId()),
        RoleId.nullableOf(entity.getParentRoleId()),
        entity.getTenantId() == null);
  }

  private RoleView toRoleView(UUID roleUuid, CreateRoleRequest request) {
    return new RoleView(
        RoleId.of(roleUuid),
        request.code(),
        request.name(),
        request.description(),
        request.tenantId(),
        request.parentRoleId(),
        request.system());
  }

  private RoleView toRoleView(UUID roleUuid, UpdateRoleRequest request) {
    return new RoleView(
        RoleId.of(roleUuid),
        request.code(),
        request.name(),
        request.description(),
        request.tenantId(),
        request.parentRoleId(),
        request.system());
  }
}

