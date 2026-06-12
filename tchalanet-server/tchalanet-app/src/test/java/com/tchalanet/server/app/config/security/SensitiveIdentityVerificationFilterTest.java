package com.tchalanet.server.app.config.security;

import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderApi;
import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SensitiveIdentityVerificationFilterTest {

  private final IdentityProviderApi identityProviderApi = mock(IdentityProviderApi.class);
  private final SensitiveIdentityVerificationFilter filter =
      new SensitiveIdentityVerificationFilter(
          identityProviderApi, new SensitiveIdentityRequestMatcher());

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void mapsSensitiveWriteWithSensitivePolicyBeforeContinuing() throws Exception {
    var authentication = authentication();
    var externalUser =
        new ExternalAuthenticatedUser(
            IdentityProviderType.FIREBASE, "issuer", "subject", null, false, Map.of());
    when(
            identityProviderApi.mapVerifiedToken(
                any(VerifiedExternalToken.class), eq(IdentityVerificationPolicy.SENSITIVE)))
        .thenReturn(externalUser);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    var chain = new MockFilterChain();

    filter.doFilter(
        new MockHttpServletRequest("POST", "/api/v1/tenant/sales/tickets"),
        new MockHttpServletResponse(),
        chain);

    verify(identityProviderApi)
        .mapVerifiedToken(any(VerifiedExternalToken.class), eq(IdentityVerificationPolicy.SENSITIVE));
    assertThat(authentication.getDetails()).isEqualTo(externalUser);
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void doesNotRemapStandardRead() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(authentication());
    var chain = new MockFilterChain();

    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/v1/tenant/cashier/home"),
        new MockHttpServletResponse(),
        chain);

    verify(identityProviderApi, never()).mapVerifiedToken(any(), any());
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void failsClosedBeforeHandlerWhenSensitiveVerificationFails() throws Exception {
    when(
            identityProviderApi.mapVerifiedToken(
                any(VerifiedExternalToken.class), eq(IdentityVerificationPolicy.SENSITIVE)))
        .thenThrow(new IdentityProviderException("revoked", "Token revoked", null));
    SecurityContextHolder.getContext().setAuthentication(authentication());
    var chain = new MockFilterChain();
    var response = new MockHttpServletResponse();

    filter.doFilter(
        new MockHttpServletRequest("POST", "/api/v1/tenant/sales/tickets"), response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(chain.getRequest()).isNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  private static JwtAuthenticationToken authentication() {
    Instant now = Instant.now();
    var jwt =
        new Jwt(
            "token",
            now,
            now.plusSeconds(300),
            Map.of("alg", "none"),
            Map.of("iss", "issuer", "sub", "subject", "aud", "project"));
    return new JwtAuthenticationToken(jwt, java.util.List.of(new SimpleGrantedAuthority("USER")));
  }
}
