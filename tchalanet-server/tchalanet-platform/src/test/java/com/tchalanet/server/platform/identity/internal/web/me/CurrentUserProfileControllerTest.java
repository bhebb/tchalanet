package com.tchalanet.server.platform.identity.internal.web.me;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.api.model.request.UpdateUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapService;
import com.tchalanet.server.platform.identity.internal.web.model.MeResponse;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("CurrentUserProfileController")
class CurrentUserProfileControllerTest {

  private final CurrentUserProfileService profiles = mock(CurrentUserProfileService.class);
  private final UserBootstrapService bootstrap = mock(UserBootstrapService.class);

  private final CurrentUserProfileController controller =
      new CurrentUserProfileController(profiles, bootstrap);

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
      var ctx = context(tenantId, userId, UUID.randomUUID().toString(), Set.of(TchRole.CASHIER));

      when(profiles.getCurrentUser(userId)).thenReturn(currentUserView(userId, tenantId));

      MeResponse response = controller.me(ctx).data();

      assertThat(response.landing().preferredSurface()).isEqualTo(ClientSurface.MOBILE_POS);
      assertThat(response.landing().availableSurfaces())
          .containsExactlyInAnyOrder(ClientSurface.MOBILE_POS, ClientSurface.CASHIER_WEB);
      assertThat(response.capabilities()).contains("cashier.sell", "cashier.print");
      assertThat(response.profileActions().canEditLocale()).isTrue();
    }
  }

  @Nested
  @DisplayName("POST /tenant/me/profile/bootstrap")
  class BootstrapProfile {

    @Test
    @DisplayName("should reject bootstrap when sub is missing")
    void shouldRejectBootstrapWhenSubMissing() {
      var tenantId = TenantId.of(UUID.randomUUID());
      var userId = UserId.of(UUID.randomUUID());
      var ctx = context(tenantId, userId, null);

      assertThatThrownBy(() -> controller.bootstrap(ctx))
          .hasMessageContaining("Missing external identity subject");
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
        "USD");
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

  private static TchRequestContext context(TenantId tenantId, UserId userId, String keycloakSub) {
    return context(tenantId, userId, keycloakSub, Set.of(TchRole.TENANT_ADMIN));
  }

  private static TchRequestContext context(
      TenantId tenantId, UserId userId, String keycloakSub, Set<TchRole> roles) {
    return new TchRequestContext(
        "tenant-demo",
        tenantId.value(),
        "tenant-demo",
        tenantId.value(),
        keycloakSub,
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
        null);
  }
}
