package com.tchalanet.server.core.accesscontrol.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.accesscontrol.application.command.model.CreateRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.command.model.GrantPermissionToRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.command.model.RevokePermissionFromRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.command.model.UpdateRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListPermissionsQuery;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListRolePermissionsQuery;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListRolesQuery;
import com.tchalanet.server.core.accesscontrol.infra.web.mapper.AccessControlWebMapper;
import com.tchalanet.server.core.accesscontrol.infra.web.model.PermissionResponse;
import com.tchalanet.server.core.accesscontrol.infra.web.model.RoleAdminResponse;
import com.tchalanet.server.core.accesscontrol.infra.web.model.UpdateRolePermissionsRequest;
import com.tchalanet.server.core.accesscontrol.infra.web.model.UpsertRoleRequest;
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

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  // --- ROLES ---

  @Operation(summary = "List roles (admin)")
  @GetMapping("/roles")
  public List<RoleAdminResponse> listRoles(@RequestParam(required = false) TenantId tenantId) {
    var roles = queryBus.ask(new ListRolesQuery(tenantId));
    return roles.stream()
        .map(AccessControlWebMapper::toRoleAdminResponse)
        .collect(Collectors.toList());
  }

  @Operation(summary = "Create or update a role (admin)")
  @PostMapping("/roles")
  public UUID upsertRole(@Valid @RequestBody UpsertRoleRequest request) {
    if (request.id() == null) {
      return commandBus.execute(
          new CreateRoleCommand(
              null,
              request.code(),
              request.name(),
              request.description(),
              TenantId.of(request.tenantId()),
              com.tchalanet.server.common.types.id.RoleId.nullableOf(request.parentRoleId()),
              request.system()));
    }
    return commandBus.execute(
        new UpdateRoleCommand(
            RoleId.of(request.id()),
            request.code(),
            request.name(),
            request.description(),
            TenantId.of(request.tenantId()),
            com.tchalanet.server.common.types.id.RoleId.nullableOf(request.parentRoleId()),
            request.system()));
  }

  // --- PERMISSIONS ---

  @Operation(summary = "List permissions (admin)")
  @GetMapping("/permissions")
  public List<PermissionResponse> listPermissions() {
    var perms = queryBus.ask(new ListPermissionsQuery());
    return perms.stream()
        .map(AccessControlWebMapper::toPermissionResponse)
        .collect(Collectors.toList());
  }

  @Operation(summary = "Get role permissions (admin)")
  @GetMapping("/roles/{roleId}/permissions")
  public Set<String> getRolePermissions(@PathVariable RoleId roleId) {
    return queryBus.ask(new ListRolePermissionsQuery(roleId));
  }

  @Operation(summary = "Update role permissions (admin)")
  @PutMapping("/roles/{roleId}/permissions")
  public void updateRolePermissions(
      @PathVariable RoleId roleId, @Valid @RequestBody UpdateRolePermissionsRequest request) {

    var current = queryBus.ask(new ListRolePermissionsQuery(roleId));
    var desired = Set.copyOf(request.permissionCodes());

    var toGrant = desired.stream().filter(p -> !current.contains(p)).toList();
    var toRevoke = current.stream().filter(p -> !desired.contains(p)).toList();

    toGrant.forEach(code -> commandBus.execute(new GrantPermissionToRoleCommand(roleId, code)));
    toRevoke.forEach(code -> commandBus.execute(new RevokePermissionFromRoleCommand(roleId, code)));
  }
}
