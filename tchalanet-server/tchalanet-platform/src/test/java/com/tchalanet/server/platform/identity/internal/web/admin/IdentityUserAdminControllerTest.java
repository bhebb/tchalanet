package com.tchalanet.server.platform.identity.internal.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import com.tchalanet.server.platform.identity.internal.model.TenantMembership;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.ExternalIdentityLinkService;
import com.tchalanet.server.platform.identity.internal.service.TenantMembershipService;
import com.tchalanet.server.platform.identity.internal.service.TenantUserAdministrationService;
import com.tchalanet.server.platform.identity.internal.service.TenantUserProvisioningService;
import com.tchalanet.server.platform.identity.internal.web.admin.model.LinkExternalIdentityRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.SetUserRoleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IdentityUserAdminController")
class IdentityUserAdminControllerTest {

    private final CurrentUserProfileService profiles = mock(CurrentUserProfileService.class);
    private final TenantUserAdministrationService users = mock(TenantUserAdministrationService.class);
    private final TenantMembershipService memberships = mock(TenantMembershipService.class);
    private final com.tchalanet.server.platform.accesscontrol.api.AccessControlApi accessControlApi =
        mock(com.tchalanet.server.platform.accesscontrol.api.AccessControlApi.class);
    private final TenantUserProvisioningService provisioning = mock(TenantUserProvisioningService.class);
    private final ExternalIdentityLinkService externalIdentities = mock(ExternalIdentityLinkService.class);
    // Real assembler over the mocked services so tenant-scoping/view composition stays exercised.
    private final TenantUserAdminViewAssembler view =
        new TenantUserAdminViewAssembler(profiles, memberships, externalIdentities);

    private final IdentityUserAdminController controller =
        new IdentityUserAdminController(
            profiles,
            users,
            memberships,
            externalIdentities,
            accessControlApi,
            provisioning, null,
            view);

    @Nested
    @DisplayName("Role restrictions")
    class RoleRestrictions {

        @Test
        @DisplayName("should forbid SUPER_ADMIN assignment when actor is tenant admin")
        void shouldForbidSuperAdminAssignmentWhenActorIsTenantAdmin() {
            var tenantId = TenantId.of(UUID.randomUUID());
            var userId = UserId.of(UUID.randomUUID());
            var ctx = context(tenantId, TchRole.TENANT_ADMIN);

            when(memberships.findByTenantAndUser(tenantId, userId))
                .thenReturn(Optional.of(TenantMembership.active(tenantId, userId)));

            assertThatThrownBy(() -> controller.setRole(ctx, userId, new SetUserRoleRequest(TchRole.SUPER_ADMIN)))
                .hasMessageContaining("Tenant admin cannot assign SUPER_ADMIN");
        }
    }

    @Nested
    @DisplayName("Tenant scoping")
    class TenantScoping {

        @Test
        @DisplayName("should forbid cross-tenant suspend when user is outside effective tenant")
        void shouldForbidCrossTenantSuspendWhenUserOutsideTenant() {
            var tenantId = TenantId.of(UUID.randomUUID());
            var userId = UserId.of(UUID.randomUUID());
            var ctx = context(tenantId, TchRole.TENANT_ADMIN);

            when(memberships.findByTenantAndUser(tenantId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.deleteMembership(ctx, userId))
                .hasMessageContaining("outside effective tenant scope");
        }

        @Test
        @DisplayName("should return scoped user details when membership exists")
        void shouldReturnScopedUserDetailsWhenMembershipExists() {
            var tenantId = TenantId.of(UUID.randomUUID());
            var userId = UserId.of(UUID.randomUUID());
            var ctx = context(tenantId, TchRole.TENANT_ADMIN);

            when(memberships.findByTenantAndUser(tenantId, userId))
                .thenReturn(Optional.of(TenantMembership.active(tenantId, userId)));
            when(memberships.findCreatedAt(tenantId, userId)).thenReturn(Optional.empty());
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

            controller.getUser(ctx, userId);
        }

        @Test
        @DisplayName("should link a provider-neutral external identity for a scoped user")
        void shouldLinkExternalIdentityForScopedUser() {
            var tenantId = TenantId.of(UUID.randomUUID());
            var userId = UserId.of(UUID.randomUUID());
            var ctx = context(tenantId, TchRole.TENANT_ADMIN);
            var request =
                new LinkExternalIdentityRequest(
                    IdentityProviderType.FIREBASE,
                    "https://securetoken.google.com/demo",
                    "firebase-uid",
                    "tenant.user@tchalanet.test");

            when(memberships.findByTenantAndUser(tenantId, userId))
                .thenReturn(Optional.of(TenantMembership.active(tenantId, userId)));
            when(memberships.findCreatedAt(tenantId, userId)).thenReturn(Optional.empty());
            when(externalIdentities.hasIdentity(userId)).thenReturn(true);
            when(profiles.getUserProfile(userId)).thenReturn(userProfile(userId));

            controller.linkExternalIdentity(ctx, userId, request);

            verify(externalIdentities)
                .link(
                    userId,
                    IdentityProviderType.FIREBASE,
                    "https://securetoken.google.com/demo",
                    "firebase-uid",
                    "tenant.user@tchalanet.test");
        }
    }

    private static UserProfileView userProfile(UserId userId) {
        return new UserProfileView(
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
            "USD");
    }

    private static TchRequestContext context(TenantId tenantId, TchRole role) {
        return new TchRequestContext(
            "tenant-demo",
            tenantId.value(),
            "tenant-demo",
            tenantId.value(),
            UUID.randomUUID().toString(),
            UUID.randomUUID(),
            Set.of(role),
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
            null,
            null, null, null, null, null);
    }
}

