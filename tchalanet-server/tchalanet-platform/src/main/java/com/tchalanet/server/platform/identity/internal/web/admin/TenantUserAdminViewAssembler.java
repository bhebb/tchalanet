package com.tchalanet.server.platform.identity.internal.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import com.tchalanet.server.platform.identity.internal.model.TenantMembership;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.TenantMembershipService;
import com.tchalanet.server.platform.identity.internal.web.admin.model.InvitationStatus;
import com.tchalanet.server.platform.identity.internal.web.admin.model.KeycloakSyncStatus;
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

  /** Fails with 403 when the target user is not a member of the request's effective tenant. */
  public void assertTenantScoped(TchRequestContext ctx, UserId userId) {
    if (memberships.findByTenantAndUser(ctx.tenantId(), userId).isEmpty()) {
      throw ProblemRest.forbidden("User is outside effective tenant scope");
    }
  }

  /** Loads and maps the full admin response for a tenant-scoped user. */
  public TenantUserAdminResponse load(
      TchRequestContext ctx, UserId userId, InvitationStatus invitationStatus, Instant createdAtOverride) {
    assertTenantScoped(ctx, userId);
    var profile = profiles.getUserProfile(userId);
    var membership = memberships.findByTenantAndUser(ctx.tenantId(), userId).orElse(null);
    var createdAt =
        createdAtOverride != null
            ? createdAtOverride
            : memberships.findCreatedAt(ctx.tenantId(), userId).orElse(null);
    return new TenantUserAdminResponse(
        profile.id(),
        profile.keycloakSub() == null ? null : profile.keycloakSub().value().toString(),
        profile.username(),
        profile.email(),
        profile.phone(),
        profile.status() == null ? null : profile.status().name(),
        null, // roles are in tenant_user_role — use /admin/access-control/users/{id}/roles
        membership == null || membership.status() == null ? null : membership.status().name(),
        membership == null ? null : membership.outletId(),
        membership == null ? null : membership.terminalId(),
        resolveSyncStatus(profile, membership),
        invitationStatus,
        createdAt,
        profile.firstName(),
        profile.lastName(),
        profile.displayName());
  }

  private static KeycloakSyncStatus resolveSyncStatus(UserProfileView profile, TenantMembership membership) {
    if (membership == null) {
      return KeycloakSyncStatus.NOT_REQUIRED;
    }
    return profile.keycloakSub() == null ? KeycloakSyncStatus.PENDING : KeycloakSyncStatus.SYNCED;
  }
}
