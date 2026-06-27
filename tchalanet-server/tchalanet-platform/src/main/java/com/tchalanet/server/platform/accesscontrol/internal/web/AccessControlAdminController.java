package com.tchalanet.server.platform.accesscontrol.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.AssignRoleToUserRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.BootstrapAccessControlRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.DenyUserPermissionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantUserPermissionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolesRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RemoveRoleFromUserRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RemoveUserPermissionOverrideRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.result.BootstrapAccessControlResult;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.PermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RolePermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RoleView;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/access-control")
@RequiredArgsConstructor
@Tag(name = "Admin • Access Control")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
public class AccessControlAdminController {

  private final AccessControlApi accessControlApi;

  // ─── Catalog reads ────────────────────────────────────────────────────────

  @Operation(summary = "List system roles")
  @GetMapping("/roles")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasPermission(null, 'role.read')")
  public ApiResponse<List<RoleView>> listRoles(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(accessControlApi.listRoles(new ListRolesRequest(ctx.tenantId())));
  }

  @Operation(summary = "List permissions")
  @GetMapping("/permissions")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasPermission(null, 'permission.read')")
  public ApiResponse<List<PermissionView>> listPermissions() {
    return ApiResponse.success(accessControlApi.listPermissions(new ListPermissionsRequest()));
  }

  @Operation(summary = "List permissions for a role")
  @GetMapping("/roles/{roleId}/permissions")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasPermission(null, 'role.read')")
  public ApiResponse<Set<String>> getRolePermissions(@PathVariable RoleId roleId) {
    var codes = accessControlApi.listRolePermissions(new ListRolePermissionsRequest(roleId))
        .stream().map(RolePermissionView::permissionCode).collect(Collectors.toSet());
    return ApiResponse.success(codes);
  }

  // ─── Effective permissions ────────────────────────────────────────────────

  @Operation(summary = "Get effective permissions for a user")
  @GetMapping("/users/{userId}/permissions/effective")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasPermission(null, 'user.read')")
  public ApiResponse<EffectivePermissionsView> getEffective(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @RequestParam(required = false, name = "tenant_id") UUID tenantId) {
    return ApiResponse.success(accessControlApi.getEffectivePermissions(
        new GetEffectivePermissionsRequest(userId, effectiveTenant(ctx, tenantId))));
  }

  // ─── Role assignment ──────────────────────────────────────────────────────

  @Operation(summary = "Assign a role to a user")
  @PostMapping("/users/{userId}/roles/{roleCode}")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasPermission(null, 'user.role.assign')")
  @AuditLog(
      action = AuditAction.USER_ROLE_CHANGE,
      entity = AuditEntityType.USER,
      idExpression = "#userId",
      tenantIdExpression = "#tenantId")
  public ApiResponse<Void> assignRole(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @PathVariable String roleCode,
      @RequestParam(required = false, name = "tenant_id") UUID tenantId) {
    accessControlApi.assignRoleToUser(
        new AssignRoleToUserRequest(effectiveTenant(ctx, tenantId), userId, roleCode, ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  @Operation(summary = "Remove a role from a user")
  @DeleteMapping("/users/{userId}/roles/{roleCode}")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasPermission(null, 'user.role.assign')")
  @AuditLog(
      action = AuditAction.USER_ROLE_CHANGE,
      entity = AuditEntityType.USER,
      idExpression = "#userId",
      tenantIdExpression = "#tenantId")
  public ApiResponse<Void> removeRole(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @PathVariable String roleCode,
      @RequestParam(required = false, name = "tenant_id") UUID tenantId) {
    accessControlApi.removeRoleFromUser(new RemoveRoleFromUserRequest(effectiveTenant(ctx, tenantId), userId, roleCode));
    return ApiResponse.success(null);
  }

  // ─── User permission overrides ────────────────────────────────────────────

  @Operation(summary = "Grant a permission override to a user")
  @PutMapping("/users/{userId}/permissions/{permissionCode}/grant")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasPermission(null, 'user.permission.manage')")
  @AuditLog(
      action = AuditAction.UPDATE,
      entity = AuditEntityType.USER,
      idExpression = "#userId",
      tenantIdExpression = "#tenantId")
  public ApiResponse<Void> grantPermission(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @PathVariable String permissionCode,
      @RequestParam(required = false, name = "tenant_id") UUID tenantId,
      @RequestBody(required = false) OverrideReasonRequest body) {
    accessControlApi.grantUserPermission(new GrantUserPermissionRequest(
        effectiveTenant(ctx, tenantId), userId, permissionCode,
        body != null ? body.reason() : null,
        ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  @Operation(summary = "Deny a permission to a user")
  @PutMapping("/users/{userId}/permissions/{permissionCode}/deny")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasPermission(null, 'user.permission.manage')")
  @AuditLog(
      action = AuditAction.UPDATE,
      entity = AuditEntityType.USER,
      idExpression = "#userId",
      tenantIdExpression = "#tenantId")
  public ApiResponse<Void> denyPermission(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @PathVariable String permissionCode,
      @RequestParam(required = false, name = "tenant_id") UUID tenantId,
      @RequestBody(required = false) OverrideReasonRequest body) {
    accessControlApi.denyUserPermission(new DenyUserPermissionRequest(
        effectiveTenant(ctx, tenantId), userId, permissionCode,
        body != null ? body.reason() : null,
        ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  @Operation(summary = "Remove a permission override from a user")
  @DeleteMapping("/users/{userId}/permissions/{permissionCode}/override")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasPermission(null, 'user.permission.manage')")
  @AuditLog(
      action = AuditAction.UPDATE,
      entity = AuditEntityType.USER,
      idExpression = "#userId",
      tenantIdExpression = "#tenantId")
  public ApiResponse<Void> removeOverride(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @PathVariable String permissionCode,
      @RequestParam(required = false, name = "tenant_id") UUID tenantId) {
    accessControlApi.removeUserPermissionOverride(
        new RemoveUserPermissionOverrideRequest(effectiveTenant(ctx, tenantId), userId, permissionCode));
    return ApiResponse.success(null);
  }

  // ─── Platform ops bootstrap ───────────────────────────────────────────────

  @Operation(summary = "Bootstrap access-control matrix (platform ops)")
  @PostMapping("/bootstrap/{mode}")
  @PreAuthorize("hasRole('SUPER_ADMIN') and hasPermission(null, 'platform.ops.execute')")
  @AuditLog(action = AuditAction.UPDATE, entity = AuditEntityType.SYSTEM, idExpression = "'access-control-bootstrap'")
  public ApiResponse<BootstrapAccessControlResult> bootstrap(@PathVariable String mode) {
    var request = new BootstrapAccessControlRequest(
        BootstrapAccessControlRequest.BootstrapMode.valueOf(mode.toUpperCase()));
    return ApiResponse.success(accessControlApi.bootstrap(request));
  }

  // ─── Inner types ─────────────────────────────────────────────────────────

  public record OverrideReasonRequest(String reason) {}

  private TenantId effectiveTenant(TchRequestContext ctx, UUID tenantId) {
    return tenantId != null ? TenantId.of(tenantId) : ctx.tenantId();
  }
}
