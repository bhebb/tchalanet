package com.tchalanet.server.platform.identity.internal.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@ExtendWith(MockitoExtension.class)
class KeycloakIdentityProviderTest {

  @Mock private JwtDecoder jwtDecoder;

  @Test
  void mapsVerifiedJwtToProviderNeutralExternalUserAndSanitizesClaims() {
    var jwt =
        new Jwt(
            "token",
            Instant.parse("2026-06-12T10:00:00Z"),
            Instant.parse("2026-06-12T11:00:00Z"),
            Map.of("alg", "RS256"),
            Map.of(
                "iss", "https://auth.example/realms/tchalanet",
                "sub", "external-subject",
                "email", "cashier@example.com",
                "email_verified", true,
                "preferred_username", "cashier",
                "realm_access", Map.of("roles", List.of("SUPER_ADMIN"))));
    when(jwtDecoder.decode("token")).thenReturn(jwt);

    var user =
        new KeycloakIdentityProvider(jwtDecoder)
            .verifyBearerToken("token", IdentityVerificationPolicy.STANDARD);

    assertThat(user.provider()).isEqualTo(IdentityProviderType.KEYCLOAK);
    assertThat(user.issuer()).isEqualTo("https://auth.example/realms/tchalanet");
    assertThat(user.subject()).isEqualTo("external-subject");
    assertThat(user.email()).isEqualTo("cashier@example.com");
    assertThat(user.emailVerified()).isTrue();
    assertThat(user.safeClaims()).containsExactly(Map.entry("preferred_username", "cashier"));
  }

  @Test
  void exposesNeutralFailureForInvalidToken() {
    when(jwtDecoder.decode("bad-token")).thenThrow(new JwtException("signature failure"));

    assertThatThrownBy(
            () ->
                new KeycloakIdentityProvider(jwtDecoder)
                    .verifyBearerToken("bad-token", IdentityVerificationPolicy.SENSITIVE))
        .isInstanceOf(IdentityProviderException.class)
        .extracting("code")
        .isEqualTo("invalid_token");
  }

  @Test
  void mapsAlreadyVerifiedTokenWithoutDecodingAgain() {
    var user =
        new KeycloakIdentityProvider(jwtDecoder)
            .mapVerifiedToken(
                new VerifiedExternalToken(
                    "https://auth.example/realms/tchalanet",
                    "external-subject",
                    "cashier@example.com",
                    true,
                    Map.of(
                        "preferred_username", "cashier",
                        "realm_access", Map.of("roles", List.of("SUPER_ADMIN")))),
                IdentityVerificationPolicy.STANDARD);

    assertThat(user.provider()).isEqualTo(IdentityProviderType.KEYCLOAK);
    assertThat(user.safeClaims()).containsExactly(Map.entry("preferred_username", "cashier"));
  }
}
