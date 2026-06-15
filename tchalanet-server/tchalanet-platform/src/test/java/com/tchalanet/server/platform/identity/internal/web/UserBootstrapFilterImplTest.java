package com.tchalanet.server.platform.identity.internal.web;

import static com.tchalanet.server.common.context.ContextKeys.BOOTSTRAPPED_APP_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.internal.service.AppUserIdentityResolution;
import com.tchalanet.server.platform.identity.internal.service.AppUserBootstrapMode;
import com.tchalanet.server.platform.identity.internal.service.ExternalIdentityAppUserResolver;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class UserBootstrapFilterImplTest {

  private final ExternalIdentityAppUserResolver appUserResolver =
      org.mockito.Mockito.mock(ExternalIdentityAppUserResolver.class);

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @EnumSource(IdentityProviderType.class)
  void bootstrapsEveryProviderThroughTheSameAppUserResolutionPath(IdentityProviderType provider)
      throws Exception {
    var externalSubject = "provider-subject";
    var appUserId = UUID.randomUUID();
    var authentication = authenticatedWith(provider, externalSubject);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(appUserResolver.resolve(any(ExternalAuthenticatedUser.class)))
        .thenReturn(Optional.of(new AppUserIdentityResolution(appUserId, UserStatus.ACTIVE)));

    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    new UserBootstrapFilterImpl(appUserResolver, properties())
        .doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(request.getAttribute(BOOTSTRAPPED_APP_USER_ID)).isEqualTo(appUserId);
    verify(appUserResolver)
        .resolve(
            org.mockito.ArgumentMatchers.argThat(
                externalUser -> externalUser.provider() == provider));
    verify(appUserResolver, never()).touchLastLogin(appUserId);
  }

  @Test
  void rejectsAuthenticatedRequestWithoutProviderNeutralIdentityDetails() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("principal", "n/a", List.of()));
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    new UserBootstrapFilterImpl(appUserResolver, properties())
        .doFilter(new MockHttpServletRequest(), response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    verify(appUserResolver, never()).resolve(any());
  }

  @Test
  void rejectsLocallySuspendedAppUserEvenWhenExternalIdentityIsAuthenticated() throws Exception {
    var appUserId = UUID.randomUUID();
    SecurityContextHolder.getContext()
        .setAuthentication(authenticatedWith(IdentityProviderType.KEYCLOAK, "provider-subject"));
    when(appUserResolver.resolve(any(ExternalAuthenticatedUser.class)))
        .thenReturn(Optional.of(new AppUserIdentityResolution(appUserId, UserStatus.SUSPENDED)));
    var response = new MockHttpServletResponse();

    new UserBootstrapFilterImpl(appUserResolver, properties())
        .doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(403);
    verify(appUserResolver, never()).touchLastLogin(appUserId);
  }

  @Test
  void rejectsUnknownExternalIdentityWithStableErrorCode() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(authenticatedWith(IdentityProviderType.FIREBASE, "unlinked-subject"));
    when(appUserResolver.resolve(any(ExternalAuthenticatedUser.class))).thenReturn(Optional.empty());
    var response = new MockHttpServletResponse();

    new UserBootstrapFilterImpl(appUserResolver, properties())
        .doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getErrorMessage()).isEqualTo("external_identity.not_linked");
  }

  private static UsernamePasswordAuthenticationToken authenticatedWith(
      IdentityProviderType provider, String subject) {
    var authentication =
        UsernamePasswordAuthenticationToken.authenticated("principal", "n/a", List.of());
    authentication.setDetails(
        new ExternalAuthenticatedUser(
            provider,
            "https://auth.example/realms/tchalanet",
            subject,
            "cashier@example.com",
            true,
            Map.of()));
    return authentication;
  }

  private static UserBootstrapProperties properties() {
    return new UserBootstrapProperties(
        true, false, AppUserBootstrapMode.DENY, List.of(), List.of(), false);
  }
}
