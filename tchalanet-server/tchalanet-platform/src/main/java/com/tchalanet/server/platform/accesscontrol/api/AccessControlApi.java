package com.tchalanet.server.platform.accesscontrol.api;

import java.util.List;

import com.tchalanet.server.platform.accesscontrol.api.model.result.CheckUserPermissionsResult;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CreateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
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
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;

public interface AccessControlApi {

    CheckUserPermissionsResult checkPermissions(CheckUserPermissionsRequest request);
    List<RoleView> listRoles(ListRolesRequest request);
    List<PermissionView> listPermissions(ListPermissionsRequest request);
    List<RolePermissionView> listRolePermissions(ListRolePermissionsRequest request);
    EffectivePermissionsView getEffectivePermissions(GetEffectivePermissionsRequest request);
    RoleView createRole(CreateRoleRequest request);
    RoleView updateRole(UpdateRoleRequest request);
    void grantPermission(GrantPermissionToRoleRequest request);
    void revokePermission(RevokePermissionFromRoleRequest request);
    void setTenantUserRole(SetTenantUserRoleRequest request);
}
