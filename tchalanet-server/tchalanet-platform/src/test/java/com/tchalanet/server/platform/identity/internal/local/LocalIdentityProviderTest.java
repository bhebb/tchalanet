package com.tchalanet.server.platform.identity.internal.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tchalanet.server.platform.identity.api.IdentityProviderApi;
import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class LocalIdentityProviderTest {

  private static final String ISSUER = "tchalanet-local";
  private static final LocalIdentityProperties PROPERTIES =
      new LocalIdentityProperties(ISSUER, "dev-only-secret-with-at-least-32-characters");

  @ParameterizedTest
  @MethodSource("providers")
  void mapsSignedLocalJwtWithoutTrustingRoles(
      IdentityProviderType expectedType, IdentityProviderApi provider, JwtDecoder decoder) {
    when(decoder.decode("token"))
        .thenReturn(
            jwt(
                Map.of(
                    "iss", ISSUER,
                    "sub", "local-subject",
                    "email", "seller@example.com",
                    "email_verified", true,
                    "roles", java.util.List.of("SUPER_ADMIN"),
                    "preferred_username", "seller")));

    var user = provider.verifyBearerToken("token", IdentityVerificationPolicy.STANDARD);

    assertThat(user.provider()).isEqualTo(expectedType);
    assertThat(user.subject()).isEqualTo("local-subject");
    assertThat(user.safeClaims()).containsOnlyKeys("preferred_username");
  }

  @ParameterizedTest
  @MethodSource("providers")
  void rejectsDecoderFailure(
      IdentityProviderType ignoredType, IdentityProviderApi provider, JwtDecoder decoder) {
    when(decoder.decode("token")).thenThrow(new JwtException("bad signature"));

    assertThatThrownBy(
            () -> provider.verifyBearerToken("token", IdentityVerificationPolicy.STANDARD))
        .isInstanceOf(IdentityProviderException.class)
        .extracting("code")
        .isEqualTo("invalid_token");
  }

  private static Stream<Arguments> providers() {
    var localJwtDecoder = Mockito.mock(JwtDecoder.class);
    var localPerfDecoder = Mockito.mock(JwtDecoder.class);
    return Stream.of(
        Arguments.of(
            IdentityProviderType.LOCAL_JWT,
            new LocalJwtIdentityProvider(localJwtDecoder, PROPERTIES),
            localJwtDecoder),
        Arguments.of(
            IdentityProviderType.LOCAL_PERF,
            new LocalPerfIdentityProvider(localPerfDecoder, PROPERTIES),
            localPerfDecoder));
  }

  private static Jwt jwt(Map<String, Object> claims) {
    return new Jwt(
        "token",
        Instant.parse("2026-06-12T10:00:00Z"),
        Instant.parse("2026-06-12T11:00:00Z"),
        Map.of("alg", "HS256"),
        claims);
  }
}
