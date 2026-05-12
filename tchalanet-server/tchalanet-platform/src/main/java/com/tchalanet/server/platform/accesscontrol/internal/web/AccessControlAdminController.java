package com.tchalanet.server.platform.accesscontrol.internal.web;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CreateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantPermissionToRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolesRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RevokePermissionFromRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.UpdateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.internal.web.mapper.AccessControlWebMapper;
import com.tchalanet.server.platform.accesscontrol.internal.web.model.PermissionResponse;
import com.tchalanet.server.platform.accesscontrol.internal.web.model.RoleAdminResponse;
import com.tchalanet.server.platform.accesscontrol.internal.web.model.UpdateRolePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.internal.web.model.UpsertRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin • Access Control")
@PreAuthorize("hasPermission('roles.manage')")
public class AccessControlAdminController {

  private final AccessControlApi accessControlApi;

  // --- ROLES ---

  @Operation(summary = "List roles (admin)")
  @GetMapping("/roles")
  public List<RoleAdminResponse> listRoles(@RequestParam(required = false) TenantId tenantId) {
    var roles = accessControlApi.listRoles(new ListRolesRequest(tenantId));
    return roles.stream()
        .map(AccessControlWebMapper::toRoleAdminResponse)
        .collect(Collectors.toList());
  }

  @Operation(summary = "Create or update a role (admin)")
  @PostMapping("/roles")
  public UUID upsertRole(@Valid @RequestBody UpsertRoleRequest request) {
    if (request.id() == null) {
      return accessControlApi
          .createRole(
              new CreateRoleRequest(
                  null,
                  request.code(),
                  request.name(),
                  request.description(),
                  TenantId.of(request.tenantId()),
                  RoleId.nullableOf(request.parentRoleId()),
                  request.system()))
          .id()
          .value();
    }
    return accessControlApi
        .updateRole(
            new UpdateRoleRequest(
                RoleId.of(request.id()),
                request.code(),
                request.name(),
                request.description(),
                TenantId.of(request.tenantId()),
                RoleId.nullableOf(request.parentRoleId()),
                request.system()))
        .id()
        .value();
  }

  // --- PERMISSIONS ---

  @Operation(summary = "List permissions (admin)")
  @GetMapping("/permissions")
  public List<PermissionResponse> listPermissions() {
    var perms = accessControlApi.listPermissions(new ListPermissionsRequest());
    return perms.stream()
        .map(AccessControlWebMapper::toPermissionResponse)
        .collect(Collectors.toList());
  }

  @Operation(summary = "Get role permissions (admin)")
  @GetMapping("/roles/{roleId}/permissions")
  public Set<String> getRolePermissions(@PathVariable RoleId roleId) {
    return accessControlApi.listRolePermissions(new ListRolePermissionsRequest(roleId)).stream()
        .map(rolePermission -> rolePermission.permissionCode())
        .collect(Collectors.toSet());
  }

  @Operation(summary = "Update role permissions (admin)")
  @PutMapping("/roles/{roleId}/permissions")
  public void updateRolePermissions(
      @PathVariable RoleId roleId, @Valid @RequestBody UpdateRolePermissionsRequest request) {

    var current =
        accessControlApi.listRolePermissions(new ListRolePermissionsRequest(roleId)).stream()
            .map(rolePermission -> rolePermission.permissionCode())
            .collect(Collectors.toSet());
    var desired = Set.copyOf(request.permissionCodes());

    var toGrant = desired.stream().filter(p -> !current.contains(p)).toList();
    var toRevoke = current.stream().filter(p -> !desired.contains(p)).toList();

    toGrant.forEach(code -> accessControlApi.grantPermission(new GrantPermissionToRoleRequest(roleId, code)));
    toRevoke.forEach(code -> accessControlApi.revokePermission(new RevokePermissionFromRoleRequest(roleId, code)));
  }
}
