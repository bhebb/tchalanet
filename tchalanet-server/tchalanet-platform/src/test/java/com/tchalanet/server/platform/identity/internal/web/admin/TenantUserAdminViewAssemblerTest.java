package com.tchalanet.server.platform.identity.internal.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import com.tchalanet.server.platform.identity.internal.model.TenantMembership;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.ExternalIdentityLinkService;
import com.tchalanet.server.platform.identity.internal.service.TenantMembershipService;
import com.tchalanet.server.platform.identity.internal.web.admin.model.InvitationStatus;
import com.tchalanet.server.platform.identity.internal.web.admin.model.ExternalIdentitySyncStatus;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TenantUserAdminViewAssembler")
class TenantUserAdminViewAssemblerTest {

  private final CurrentUserProfileService profiles = mock(CurrentUserProfileService.class);
  private final TenantMembershipService memberships = mock(TenantMembershipService.class);
  private final ExternalIdentityLinkService externalIdentities = mock(ExternalIdentityLinkService.class);
  private final TenantUserAdminViewAssembler assembler =
      new TenantUserAdminViewAssembler(profiles, memberships, externalIdentities);

  @Test
  @DisplayName("assertTenantScoped throws 403 when the user is not a member of the effective tenant")
  void assertTenantScopedRejectsOutsideUser() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var userId = UserId.of(UUID.randomUUID());
    var ctx = context(tenantId);
    when(memberships.findByTenantAndUser(tenantId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> assembler.assertTenantScoped(ctx, userId))
        .hasMessageContaining("outside effective tenant scope");
  }

  @Test
  @DisplayName("load composes profile + membership + sync status, omits role, and honours createdAt override")
  void loadComposesView() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var userId = UserId.of(UUID.randomUUID());
    var outletId = OutletId.of(UUID.randomUUID());
    var terminalId = TerminalId.of(UUID.randomUUID());
    var ctx = context(tenantId);
    var createdAt = Instant.parse("2026-01-02T03:04:05Z");

    when(memberships.findByTenantAndUser(tenantId, userId))
        .thenReturn(Optional.of(TenantMembership.active(tenantId, userId).assign(outletId, terminalId, false)));
    when(externalIdentities.hasIdentity(userId)).thenReturn(true);
    when(profiles.getUserProfile(userId))
        .thenReturn(
            new UserProfileView(
                userId,
                "tenant.user",
                "tenant.user@tchalanet.test",
                "+5090000",
                UserStatus.ACTIVE,
                "Tenant",
                "User",
                "Tenant User",
                null,
                null,
                "fr",
                "America/Port-au-Prince",
                "USD"));

    var response = assembler.load(ctx, userId, InvitationStatus.SENT, createdAt);

    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.username()).isEqualTo("tenant.user");
    assertThat(response.status()).isEqualTo("ACTIVE");
    assertThat(response.role()).isNull(); // roles are served by /admin/access-control/users/{id}/roles
    assertThat(response.membershipStatus()).isEqualTo("ACTIVE");
    assertThat(response.outletId()).isEqualTo(outletId);
    assertThat(response.terminalId()).isEqualTo(terminalId);
    assertThat(response.externalIdentitySyncStatus()).isEqualTo(ExternalIdentitySyncStatus.SYNCED);
    assertThat(response.invitationStatus()).isEqualTo(InvitationStatus.SENT);
    assertThat(response.createdAt()).isEqualTo(createdAt);
  }

  private static TchRequestContext context(TenantId tenantId) {
    return new TchRequestContext(
        "tenant-demo",
        tenantId.value(),
        "tenant-demo",
        tenantId.value(),
        UUID.randomUUID().toString(),
        UUID.randomUUID(),
        Set.of(TchRole.TENANT_ADMIN),
        Set.of(),
        Locale.FRANCE,
        "req-test",
        "127.0.0.1",
        null,
        false,
        null,
        "active",
        ApiScope.ADMIN,
        null,
        tenantId,
        java.time.ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("USD"),
        null);
  }
}
