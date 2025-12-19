package com.tchalanet.server.core.accesscontrol.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.accesscontrol.application.command.model.CreateRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.command.model.GrantPermissionToRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.command.model.RevokePermissionFromRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.command.model.UpdateRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListPermissionsQuery;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListRolePermissionsQuery;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListRolesQuery;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.PermissionResponse;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.RoleAdminResponse;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.UpdateRolePermissionsRequest;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.UpsertRoleRequest;
import com.tchalanet.server.core.accesscontrol.infra.web.mapper.AccessControlWebMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AccessControlAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  // --- ROLES ---

  @GetMapping("/roles")
  @RequiresPermission("roles.manage")
  public List<RoleAdminResponse> listRoles(@RequestParam(required = false) UUID tenantId) {
    var roles = queryBus.send(new ListRolesQuery(tenantId));
    return roles.stream()
        .map(AccessControlWebMapper::toRoleAdminResponse)
        .collect(Collectors.toList());
  }

  @PostMapping("/roles")
  @RequiresPermission("roles.manage")
  public UUID upsertRole(@Valid @RequestBody UpsertRoleRequest request) {
    if (request.id() == null) {
      return commandBus.send(
          new CreateRoleCommand(
              null,
              request.code(),
              request.name(),
              request.description(),
              request.tenantId(),
              request.parentRoleId(),
              request.system()));
    }
    return commandBus.send(
        new UpdateRoleCommand(
            request.id(),
            request.code(),
            request.name(),
            request.description(),
            request.tenantId(),
            request.parentRoleId(),
            request.system()));
  }

  // --- PERMISSIONS ---

  @GetMapping("/permissions")
  @RequiresPermission("roles.manage")
  public List<PermissionResponse> listPermissions() {
    var perms = queryBus.ask(new ListPermissionsQuery());
    return perms.stream()
        .map(AccessControlWebMapper::toPermissionResponse)
        .collect(Collectors.toList());
  }

  @GetMapping("/roles/{roleId}/permissions")
  @RequiresPermission("roles.manage")
  public Set<String> getRolePermissions(@PathVariable UUID roleId) {
    return queryBus.ask(new ListRolePermissionsQuery(roleId));
  }

  @PutMapping("/roles/{roleId}/permissions")
  @RequiresPermission("roles.manage")
  public void updateRolePermissions(
      @PathVariable UUID roleId, @Valid @RequestBody UpdateRolePermissionsRequest request) {

    var current = queryBus.ask(new ListRolePermissionsQuery(roleId));
    var desired = Set.copyOf(request.permissionCodes());

    var toGrant = desired.stream().filter(p -> !current.contains(p)).toList();
    var toRevoke = current.stream().filter(p -> !desired.contains(p)).toList();

    toGrant.forEach(code -> commandBus.send(new GrantPermissionToRoleCommand(roleId, code)));
    toRevoke.forEach(code -> commandBus.send(new RevokePermissionFromRoleCommand(roleId, code)));
  }
}
