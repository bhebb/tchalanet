package com.tchalanet.server.core.accesscontrol.infra.web;

import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.accesscontrol.application.port.in.PermissionAdminUseCase;
import com.tchalanet.server.core.accesscontrol.application.port.in.RoleAdminUseCase;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.PermissionResponse;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.RoleAdminResponse;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.UpdateRolePermissionsRequest;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.UpsertRoleRequest;
import com.tchalanet.server.core.accesscontrol.infra.web.mapper.AccessControlWebMapper;
import jakarta.validation.Valid;
import java.util.List;
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

  private final RoleAdminUseCase roleAdminUseCase;
  private final PermissionAdminUseCase permissionAdminUseCase;

  // --- ROLES ---

  @GetMapping("/roles")
  @RequiresPermission("roles.manage")
  public List<RoleAdminResponse> listRoles(@RequestParam(required = false) UUID tenantId) {
    var roles = roleAdminUseCase.listRoles(tenantId);
    return roles.stream()
        .map(AccessControlWebMapper::toRoleAdminResponse)
        .collect(Collectors.toList());
  }

  @PostMapping("/roles")
  @RequiresPermission("roles.manage")
  public UUID upsertRole(@Valid @RequestBody UpsertRoleRequest request) {
    return roleAdminUseCase.upsertRole(
        request.id(),
        request.code(),
        request.name(),
        request.description(),
        request.tenantId(),
        request.parentRoleId(),
        request.system());
  }

  // --- PERMISSIONS ---

  @GetMapping("/permissions")
  @RequiresPermission("roles.manage")
  public List<PermissionResponse> listPermissions() {
    var perms = permissionAdminUseCase.listPermissions();
    return perms.stream()
        .map(AccessControlWebMapper::toPermissionResponse)
        .collect(Collectors.toList());
  }

  @GetMapping("/roles/{roleId}/permissions")
  @RequiresPermission("roles.manage")
  public java.util.Set<String> getRolePermissions(@PathVariable UUID roleId) {
    return permissionAdminUseCase.getRolePermissions(roleId);
  }

  @PutMapping("/roles/{roleId}/permissions")
  @RequiresPermission("roles.manage")
  public void updateRolePermissions(
      @PathVariable UUID roleId, @Valid @RequestBody UpdateRolePermissionsRequest request) {
    permissionAdminUseCase.setRolePermissions(roleId, request.permissionCodes());
  }
}
