package com.tchalanet.server.platform.accesscontrol.internal.adapter;

import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.AssignRoleToUserRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.BootstrapAccessControlRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CreateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.DenyUserPermissionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantPermissionToRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantUserPermissionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolesRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RemoveRoleFromUserRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RemoveUserPermissionOverrideRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RevokePermissionFromRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.SetTenantUserRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.UpdateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.result.BootstrapAccessControlResult;
import com.tchalanet.server.platform.accesscontrol.api.model.result.CheckUserPermissionsResult;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.PermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RolePermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RoleView;
import com.tchalanet.server.platform.accesscontrol.internal.service.AccessControlBootstrapService;
import com.tchalanet.server.platform.accesscontrol.internal.service.EffectivePermissionService;
import com.tchalanet.server.platform.accesscontrol.internal.service.PermissionRegistryService;
import com.tchalanet.server.platform.accesscontrol.internal.service.RoleCatalogService;
import com.tchalanet.server.platform.accesscontrol.internal.service.TenantUserRoleService;
import com.tchalanet.server.platform.accesscontrol.internal.service.UserPermissionOverrideService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing the public {@link AccessControlApi} contract.
 *
 * <p>Bridges the public module API to the internal access-control services. Holds no business
 * logic of its own — it only delegates to the capability's internal services.
 */
@Component
@RequiredArgsConstructor
public class AccessControlApiAdapter implements AccessControlApi {

  private final EffectivePermissionService effectivePermissions;
  private final PermissionRegistryService permissionRegistry;
  private final RoleCatalogService roleCatalog;
  private final TenantUserRoleService tenantUserRoles;
  private final UserPermissionOverrideService userOverrides;
  private final AccessControlBootstrapService bootstrapService;

  // ─── Permission evaluation ────────────────────────────────────────────────

  @Override
  public CheckUserPermissionsResult checkPermissions(CheckUserPermissionsRequest request) {
    return effectivePermissions.checkPermissions(request);
  }

  @Override
  public EffectivePermissionsView getEffectivePermissions(GetEffectivePermissionsRequest request) {
    return effectivePermissions.getEffectivePermissions(request);
  }

  // ─── Catalog reads ────────────────────────────────────────────────────────

  @Override
  public List<RoleView> listRoles(ListRolesRequest request) {
    return roleCatalog.listRoles(request);
  }

  @Override
  public List<PermissionView> listPermissions(ListPermissionsRequest request) {
    return permissionRegistry.listPermissions(request);
  }

  @Override
  public List<RolePermissionView> listRolePermissions(ListRolePermissionsRequest request) {
    return roleCatalog.listRolePermissions(request);
  }

  // ─── Role assignment ──────────────────────────────────────────────────────

  @Override
  public void assignRoleToUser(AssignRoleToUserRequest request) {
    tenantUserRoles.assignRoleToUser(request);
  }

  @Override
  public void removeRoleFromUser(RemoveRoleFromUserRequest request) {
    tenantUserRoles.removeRoleFromUser(request);
  }

  // ─── User permission overrides ────────────────────────────────────────────

  @Override
  public void grantUserPermission(GrantUserPermissionRequest request) {
    userOverrides.grantUserPermission(request);
  }

  @Override
  public void denyUserPermission(DenyUserPermissionRequest request) {
    userOverrides.denyUserPermission(request);
  }

  @Override
  public void removeUserPermissionOverride(RemoveUserPermissionOverrideRequest request) {
    userOverrides.removeUserPermissionOverride(request);
  }

  // ─── Bootstrap ───────────────────────────────────────────────────────────

  @Override
  public BootstrapAccessControlResult bootstrap(BootstrapAccessControlRequest request) {
    return bootstrapService.execute(request);
  }

  // ─── Legacy role catalog management ──────────────────────────────────────

  @Override
  public RoleView createRole(CreateRoleRequest request) {
    return roleCatalog.createRole(request);
  }

  @Override
  public RoleView updateRole(UpdateRoleRequest request) {
    return roleCatalog.updateRole(request);
  }

  @Override
  public void grantPermission(GrantPermissionToRoleRequest request) {
    roleCatalog.grantPermission(request);
  }

  @Override
  public void revokePermission(RevokePermissionFromRoleRequest request) {
    roleCatalog.revokePermission(request);
  }

  @Override
  @Deprecated
  public void setTenantUserRole(SetTenantUserRoleRequest request) {
    tenantUserRoles.setTenantUserRole(request);
  }
}
