package com.tchalanet.server.platform.identity.internal.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantAdminGlobalRow;
import com.tchalanet.server.platform.identity.internal.model.TenantMembership;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.ExternalIdentityLinkService;
import com.tchalanet.server.platform.identity.internal.service.TenantMembershipService;
import com.tchalanet.server.platform.identity.internal.web.admin.model.InvitationStatus;
import com.tchalanet.server.platform.identity.internal.web.admin.model.ExternalIdentitySyncStatus;
import com.tchalanet.server.platform.identity.internal.web.admin.model.TenantUserAdminResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Composes the admin-facing tenant-user view (profile + membership + invitation/sync status) and
 * enforces tenant scoping. Kept out of the controller so the controller stays thin and the
 * composition has a single home. Roles/effective permissions are intentionally not inlined here —
 * they are served by {@code /admin/access-control/users/{id}/roles} (tenant_user_role).
 */
@Component
@RequiredArgsConstructor
public class TenantUserAdminViewAssembler {

  private final CurrentUserProfileService profiles;
  private final TenantMembershipService memberships;
  private final ExternalIdentityLinkService externalIdentities;

  /**
   * Fails with 403 when the target user is not a member of the effective tenant.
   * SUPER_ADMIN without tenant context bypasses this check (global action).
   */
  public void assertTenantScoped(TchRequestContext ctx, UserId userId) {
    if (ctx.isSuperAdmin() && !ctx.hasTenant()) return;
    if (memberships.findByTenantAndUser(ctx.effectiveTenantIdRequired(), userId).isEmpty()) {
      throw ProblemRest.forbidden("User is outside effective tenant scope");
    }
  }

  /**
   * Loads and maps the full admin response for a user.
   * When SUPER_ADMIN has no tenant context, membership info is omitted (profile only).
   */
  public TenantUserAdminResponse load(
      TchRequestContext ctx, UserId userId, InvitationStatus invitationStatus, Instant createdAtOverride) {
    assertTenantScoped(ctx, userId);
    var profile = profiles.getUserProfile(userId);
    if (ctx.isSuperAdmin() && !ctx.hasTenant()) {
      return new TenantUserAdminResponse(
          profile.id(), profile.username(), profile.email(), profile.phone(),
          profile.status() == null ? null : profile.status().name(),
          null, null, ExternalIdentitySyncStatus.NOT_REQUIRED,
          invitationStatus, createdAtOverride,
          profile.firstName(), profile.lastName(), profile.displayName(),
          null, null, null);
    }
    var tenantId = ctx.effectiveTenantIdRequired();
    var membership = memberships.findByTenantAndUser(tenantId, userId).orElse(null);
    var createdAt =
        createdAtOverride != null
            ? createdAtOverride
            : memberships.findCreatedAt(tenantId, userId).orElse(null);
    return new TenantUserAdminResponse(
        profile.id(),
        profile.username(),
        profile.email(),
        profile.phone(),
        profile.status() == null ? null : profile.status().name(),
        null, // roles are in tenant_user_role — use /admin/access-control/users/{id}/roles
        membership == null || membership.status() == null ? null : membership.status().name(),
        resolveSyncStatus(userId, membership),
        invitationStatus,
        createdAt,
        profile.firstName(),
        profile.lastName(),
        profile.displayName(),
        null, null, null);
  }

  /** Builds a cross-tenant response from a native SQL projection row (SUPER_ADMIN global list/detail). */
  public TenantUserAdminResponse fromGlobalRow(TenantAdminGlobalRow r) {
    return new TenantUserAdminResponse(
        UserId.of(r.getUserId()), null, r.getEmail(), null, r.getStatus(),
        null, null, ExternalIdentitySyncStatus.NOT_REQUIRED, InvitationStatus.NOT_SENT,
        r.getAssignedAt(), null, null, r.getDisplayName(),
        r.getTenantId() != null ? r.getTenantId().toString() : null,
        r.getTenantName(), r.getTenantCode());
  }

  private ExternalIdentitySyncStatus resolveSyncStatus(
      UserId userId, TenantMembership membership) {
    if (membership == null) {
      return ExternalIdentitySyncStatus.NOT_REQUIRED;
    }
    return !externalIdentities.hasIdentity(userId)
        ? ExternalIdentitySyncStatus.PENDING
        : ExternalIdentitySyncStatus.SYNCED;
  }
}
