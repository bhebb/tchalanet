package com.tchalanet.server.platform.identity.internal.web.platform;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.accesscontrol.internal.service.PlatformUserRoleService;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.identity.internal.service.TenantUserAdministrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/super-admins")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Platform • Super Admins")
public class PlatformSuperAdminController {

  private final TenantUserAdministrationService users;
  private final PlatformUserRoleService platformUserRoles;

  @GetMapping
  @Operation(summary = "List platform super admins")
  public ApiResponse<List<PlatformSuperAdminView>> list() {
    return ApiResponse.success(platformUserRoles.listSuperAdmins().stream()
        .map(row -> new PlatformSuperAdminView(
            row.getUserId().toString(),
            row.getEmail(),
            row.getDisplayName(),
            row.getStatus(),
            row.getAssignedAt()))
        .toList());
  }

  @PostMapping
  @Operation(summary = "Create a platform super admin")
  @AuditLog(action = AuditAction.USER_CREATE, entity = AuditEntityType.USER, idExpression = "#result.data().id()")
  public ApiResponse<PlatformSuperAdminView> create(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody CreatePlatformSuperAdminRequest request) {
    var names = splitDisplayName(request.displayName());
    var created = users.createUser(
        request.email(),
        request.phoneNumber(),
        names.firstName(),
        names.lastName(),
        null,
        null,
        null,
        null,
        null,
        request.sendInvite(),
        java.util.Set.of());
    platformUserRoles.assignSuperAdmin(created.userId(), ctx.currentUserIdRequired());
    var profile = users.profile(created.userId());
    return ApiResponse.success(new PlatformSuperAdminView(
        created.userId().value().toString(),
        profile.email(),
        profile.displayName(),
        profile.status(),
        Instant.now()));
  }

  @DeleteMapping("/{userId}")
  @Operation(summary = "Revoke platform super admin access")
  @AuditLog(action = AuditAction.USER_ROLE_CHANGE, entity = AuditEntityType.USER, idExpression = "#userId")
  public ApiResponse<Void> revoke(@PathVariable UserId userId) {
    platformUserRoles.removeSuperAdmin(userId);
    return ApiResponse.<Void>success(null);
  }

  @PatchMapping("/{userId}/suspend")
  @Operation(summary = "Suspend a platform super admin user")
  @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
  public ApiResponse<Void> suspend(@PathVariable UserId userId) {
    users.suspendUser(userId);
    return ApiResponse.<Void>success(null);
  }

  @PatchMapping("/{userId}/reactivate")
  @Operation(summary = "Reactivate a platform super admin user")
  @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
  public ApiResponse<Void> reactivate(@PathVariable UserId userId) {
    users.reactivateUser(userId);
    return ApiResponse.<Void>success(null);
  }

  private static Names splitDisplayName(String displayName) {
    var trimmed = displayName == null ? "" : displayName.trim();
    if (trimmed.isBlank()) {
      return new Names(null, null);
    }
    var parts = trimmed.split("\\s+", 2);
    return new Names(parts[0], parts.length > 1 ? parts[1] : null);
  }

  private record Names(String firstName, String lastName) {}

  public record CreatePlatformSuperAdminRequest(
      @Email @NotBlank String email,
      @NotBlank String displayName,
      String phoneNumber,
      boolean sendInvite) {}

  public record PlatformSuperAdminView(
      String id,
      String email,
      String displayName,
      String status,
      Instant assignedAt) {}
}
