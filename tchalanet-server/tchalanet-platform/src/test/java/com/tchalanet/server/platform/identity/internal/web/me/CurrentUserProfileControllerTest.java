package com.tchalanet.server.platform.identity.internal.web.me;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.api.model.request.UpdateUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.web.model.MeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CurrentUserProfileController")
@ExtendWith(MockitoExtension.class)
class CurrentUserProfileControllerTest {

    @Mock
    private CurrentUserProfileService profiles;

    @InjectMocks
    private CurrentUserProfileController controller;

    @Nested
    @DisplayName("GET /tenant/me/profile")
    class GetProfile {

        @Test
        @DisplayName("should return current profile when app user exists")
        void shouldReturnCurrentProfileWhenAppUserExists() {
            var tenantId = TenantId.of(UUID.randomUUID());
            var userId = UserId.of(UUID.randomUUID());
            var ctx = context(tenantId, userId, UUID.randomUUID().toString());

            when(profiles.getCurrentUser(userId)).thenReturn(currentUserView(userId, tenantId));

            MeResponse response = controller.me(ctx).data();

            assertThat(response.id()).isEqualTo(userId);
            assertThat(response.isNew()).isFalse();
        }

        @Test
        @DisplayName("should return profile landing surfaces")
        void profile_returns_available_surfaces() {
            var tenantId = TenantId.of(UUID.randomUUID());
            var userId = UserId.of(UUID.randomUUID());
            var ctx = context(tenantId, userId, UUID.randomUUID().toString(), Set.of(TchRole.TENANT_ADMIN));

            when(profiles.getCurrentUser(userId)).thenReturn(currentUserView(userId, tenantId));

            MeResponse response = controller.me(ctx).data();

            assertThat(response.landing().preferredSurface()).isEqualTo(ClientSurface.TENANT_ADMIN_WEB);
            assertThat(response.landing().availableSurfaces())
                .containsExactly(ClientSurface.TENANT_ADMIN_WEB);
            assertThat(response.capabilities()).isEmpty();
            assertThat(response.profileActions().canEditLocale()).isTrue();
        }
    }

    @Nested
    @DisplayName("PATCH /tenant/me/profile")
    class PatchProfile {

        @Test
        @DisplayName("should patch only allowed fields and keep email immutable")
        void shouldPatchOnlyAllowedFieldsAndKeepEmailImmutable() {
            var tenantId = TenantId.of(UUID.randomUUID());
            var userId = UserId.of(UUID.randomUUID());
            var ctx = context(tenantId, userId, UUID.randomUUID().toString());
            var request =
                new com.tchalanet.server.platform.identity.internal.web.model.UpdateUserProfileRequest(
                    "Jean", "Dupont", "+50912345678", "fr");

            when(profiles.getUserProfile(userId)).thenReturn(userProfileView(userId));

            controller.updateProfile(ctx, request);

            ArgumentCaptor<UpdateUserProfileRequest> captor =
                ArgumentCaptor.forClass(UpdateUserProfileRequest.class);
            verify(profiles).updateProfile(captor.capture());
            assertThat(captor.getValue().email()).isEmpty();
        }
    }

    private static CurrentUserView currentUserView(UserId userId, TenantId tenantId) {
        return new CurrentUserView(
            userId,
            "tenant.user",
            "tenant.user@tchalanet.test",
            "Tenant",
            "User",
            "Tenant User",
            tenantId,
            "tenant-demo",
            "America/Port-au-Prince",
            "USD",
            null,
            (short) 1,
            "fr",
            "America/Port-au-Prince",
            "USD",
            false,
            false,
            null,
            null);
    }

    private static UserProfileView userProfileView(UserId userId) {
        return new UserProfileView(
            userId,
            "tenant.user",
            "tenant.user@tchalanet.test",
            "+50912345678",
            UserStatus.ACTIVE,
            "Tenant",
            "User",
            "Tenant User",
            null,
            (short) 1,
            "fr",
            "America/Port-au-Prince",
            "USD");
    }

    private static TchRequestContext context(TenantId tenantId, UserId userId, String externalSubject) {
        return context(tenantId, userId, externalSubject, Set.of(TchRole.TENANT_ADMIN));
    }

    private static TchRequestContext context(
        TenantId tenantId, UserId userId, String externalSubject, Set<TchRole> roles) {
        return new TchRequestContext(
            "tenant-demo",
            tenantId.value(),
            "tenant-demo",
            tenantId.value(),
            userId.value(),
            roles,
            Set.of(),
            Locale.FRANCE,
            "req-test",
            "127.0.0.1",
            null,
            false,
            null,
            "active",
            ApiScope.TENANT,
            null,
            tenantId,
            java.time.ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("USD"),
            null,
            null, null, null, null, externalSubject);
    }
}
