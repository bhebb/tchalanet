package com.tchalanet.server.platform.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExternalAuthenticatedUserTest {

  @Test
  void copiesSafeClaimsAndExposesNoAuthorizationFacts() {
    var claims = new HashMap<String, Object>();
    claims.put("preferred_username", "cashier");

    var user =
        new ExternalAuthenticatedUser(
            IdentityProviderType.KEYCLOAK,
            "https://auth.example/realms/tchalanet",
            "external-subject",
            "cashier@example.com",
            true,
            claims);

    claims.put("preferred_username", "changed");

    assertThat(user.safeClaims()).isEqualTo(Map.of("preferred_username", "cashier"));
    assertThatThrownBy(() -> user.safeClaims().put("role", "SUPER_ADMIN"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsMissingStableIdentityFields() {
    assertThatThrownBy(
            () ->
                new ExternalAuthenticatedUser(
                    IdentityProviderType.KEYCLOAK, "issuer", " ", null, false, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("subject is required");
  }
}

