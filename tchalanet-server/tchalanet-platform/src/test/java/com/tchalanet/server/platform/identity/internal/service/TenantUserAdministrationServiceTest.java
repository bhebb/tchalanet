package com.tchalanet.server.platform.identity.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityProvisioningApi;
import com.tchalanet.server.platform.identity.api.ProvisionExternalUserRequest;
import com.tchalanet.server.platform.identity.api.ProvisionedExternalUser;
import com.tchalanet.server.platform.identity.internal.model.AppUser;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.UserPreferenceJpaAdapter;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TenantUserAdministrationServiceTest {

  private final AppUserJpaAdapter users = mock(AppUserJpaAdapter.class);
  private final UserPreferenceJpaAdapter preferences = mock(UserPreferenceJpaAdapter.class);
  private final TimeProvider timeProvider = mock(TimeProvider.class);
  private final IdentityProvisioningApi identityProvisioning = mock(IdentityProvisioningApi.class);
  private final ExternalIdentityLinkService externalIdentityLinks =
      mock(ExternalIdentityLinkService.class);
  private final TemporaryCredentialService temporaryCredentials =
      mock(TemporaryCredentialService.class);

  private final TenantUserAdministrationService service =
      new TenantUserAdministrationService(
          users,
          preferences,
          timeProvider,
          identityProvisioning,
          externalIdentityLinks,
          temporaryCredentials);

  @Test
  void tenantUserCreationRequestsFirebaseSubjectFromAppUserId() {
    when(users.findByEmailOrPhone("admin@example.test", null)).thenReturn(Optional.empty());
    when(timeProvider.nowInstant()).thenReturn(Instant.parse("2026-07-14T00:00:00Z"));
    when(temporaryCredentials.adminTemporaryCredentialsEnabled()).thenReturn(false);
    when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(identityProvisioning.provisionUser(any()))
        .thenAnswer(
            invocation -> {
              var request = invocation.getArgument(0, ProvisionExternalUserRequest.class);
              return new ProvisionedExternalUser(
                  IdentityProviderType.FIREBASE,
                  "https://securetoken.google.com/demo",
                  request.requestedExternalSubject(),
                  true);
            });

    var result =
        service.createUserForTenant("admin@example.test", null, "Ada", "Min", "flowtenant");

    var requestCaptor = ArgumentCaptor.forClass(ProvisionExternalUserRequest.class);
    verify(identityProvisioning).provisionUser(requestCaptor.capture());
    var requestedSubject = requestCaptor.getValue().requestedExternalSubject();

    assertThat(requestedSubject).isEqualTo(result.userId().value().toString());
    verify(externalIdentityLinks)
        .link(
            eq(result.userId()),
            eq(IdentityProviderType.FIREBASE),
            eq("https://securetoken.google.com/demo"),
            eq(requestedSubject),
            eq("admin@example.test"));
  }
}
