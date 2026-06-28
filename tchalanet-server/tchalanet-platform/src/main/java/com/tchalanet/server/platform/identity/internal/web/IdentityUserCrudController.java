package com.tchalanet.server.platform.identity.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.identity.internal.service.IdentityUserCrudService;
import com.tchalanet.server.platform.identity.internal.service.TenantUserAdministrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/identity/users")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Identity • User CRUD")
public class IdentityUserCrudController {

  private final IdentityUserCrudService service;
  private final TenantUserAdministrationService users;

  // ── Search ────────────────────────────────────────────────────────────────

  @GetMapping
  @PreAuthorize("hasPermission(null, 'user.read')")
  @Operation(summary = "Search users. unassigned=true returns users with no tenant assignment.")
  public ApiResponse<List<IdentityUserView>> search(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "false") boolean unassigned,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    if (unassigned) {
      var results = service.searchUnassigned(q, page, size).stream()
          .map(u -> new IdentityUserView(
              u.id().value().toString(), u.email(), u.displayName(), u.status().name()))
          .toList();
      return ApiResponse.success(results);
    }
    return ApiResponse.success(List.of());
  }

  // ── Create ────────────────────────────────────────────────────────────────

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasPermission(null, 'user.create')")
  @Operation(summary = "Create a user account without tenant assignment")
  @AuditLog(action = AuditAction.USER_CREATE, entity = AuditEntityType.USER, idExpression = "#result.data().id()")
  public ApiResponse<IdentityUserView> create(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody CreateIdentityUserRequest request) {
    var names = splitDisplayName(request.displayName());
    var userId = service.createUser(request.email(), request.phoneNumber(), names.firstName(), names.lastName());
    var profile = users.profile(userId);
    return ApiResponse.success(new IdentityUserView(
        userId.value().toString(), profile.email(), profile.displayName(), profile.status()));
  }

  // ── Membership ────────────────────────────────────────────────────────────

  @PostMapping("/{userId}/membership")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasPermission(null, 'user.membership.manage')")
  @Operation(summary = "Assign a user to a tenant with a role")
  @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
  public void assignMembership(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UUID userId,
      @Valid @RequestBody AssignMembershipRequest request) {
    TchRole role;
    try {
      role = TchRole.valueOf(request.role());
    } catch (IllegalArgumentException e) {
      throw com.tchalanet.server.common.web.error.ProblemRest.badRequest("Unknown role: " + request.role());
    }
    service.assignMembership(UserId.of(userId), TenantId.of(request.tenantId()), role, ctx);
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  @PostMapping("/{userId}/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasPermission(null, 'user.activate')")
  @Operation(summary = "Activate a user account")
  @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
  public void activate(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UUID userId) {
    service.activate(UserId.of(userId), ctx);
  }

  @PostMapping("/{userId}/suspend")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasPermission(null, 'user.suspend')")
  @Operation(summary = "Suspend a user account")
  @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
  public void suspend(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UUID userId) {
    service.suspend(UserId.of(userId), ctx);
  }

  @DeleteMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasPermission(null, 'user.archive')")
  @Operation(summary = "Archive (soft-delete) a user account")
  @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
  public void archive(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UUID userId) {
    service.archive(UserId.of(userId), ctx);
  }

  @PostMapping("/{userId}/reset-password")
  @PreAuthorize("hasPermission(null, 'user.password.reset')")
  @Operation(summary = "Generate and send a temporary password to the user (no reset link)")
  @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
  public ApiResponse<PasswordResetResult> resetPassword(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UUID userId) {
    var tempPassword = service.resetPassword(UserId.of(userId), ctx);
    return ApiResponse.success(new PasswordResetResult(tempPassword));
  }

  // ── Request / response records ────────────────────────────────────────────

  public record CreateIdentityUserRequest(
      @Email @NotBlank String email,
      @NotBlank String displayName,
      String phoneNumber) {}

  public record AssignMembershipRequest(
      @NotNull UUID tenantId,
      @NotBlank String role) {}

  public record IdentityUserView(String id, String email, String displayName, String status) {}

  public record PasswordResetResult(String tempPassword) {}

  // ── Utils ─────────────────────────────────────────────────────────────────

  private static Names splitDisplayName(String displayName) {
    var trimmed = displayName == null ? "" : displayName.trim();
    if (trimmed.isBlank()) return new Names(null, null);
    var parts = trimmed.split("\\s+", 2);
    return new Names(parts[0], parts.length > 1 ? parts[1] : null);
  }

  private record Names(String firstName, String lastName) {}
}
