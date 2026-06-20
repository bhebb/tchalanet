package com.tchalanet.server.platform.accesscontrol.api;

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
import com.tchalanet.server.platform.accesscontrol.api.model.view.AccessSnapshotView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.PermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RolePermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RoleView;
import com.tchalanet.server.common.types.id.UserId;
import java.util.List;

public interface AccessControlApi {

    // Permission checks (used by TchPermissionEvaluator)
    CheckUserPermissionsResult checkPermissions(CheckUserPermissionsRequest request);

    // Catalog reads
    List<RoleView> listRoles(ListRolesRequest request);
    List<PermissionView> listPermissions(ListPermissionsRequest request);
    List<RolePermissionView> listRolePermissions(ListRolePermissionsRequest request);
    EffectivePermissionsView getEffectivePermissions(GetEffectivePermissionsRequest request);
    AccessSnapshotView resolveUserAccess(UserId userId);

    // Role assignment (operates on tenant_user_role)
    void assignRoleToUser(AssignRoleToUserRequest request);
    void removeRoleFromUser(RemoveRoleFromUserRequest request);

    // User permission overrides (GRANT / DENY)
    void grantUserPermission(GrantUserPermissionRequest request);
    void denyUserPermission(DenyUserPermissionRequest request);
    void removeUserPermissionOverride(RemoveUserPermissionOverrideRequest request);

    // Bootstrap
    BootstrapAccessControlResult bootstrap(BootstrapAccessControlRequest request);

    // Legacy role catalog management (platform ops only — V1 read-only for tenant admins)
    RoleView createRole(CreateRoleRequest request);
    RoleView updateRole(UpdateRoleRequest request);
    void grantPermission(GrantPermissionToRoleRequest request);
    void revokePermission(RevokePermissionFromRoleRequest request);

    /** @deprecated use assignRoleToUser instead */
    @Deprecated
    void setTenantUserRole(SetTenantUserRoleRequest request);
}
