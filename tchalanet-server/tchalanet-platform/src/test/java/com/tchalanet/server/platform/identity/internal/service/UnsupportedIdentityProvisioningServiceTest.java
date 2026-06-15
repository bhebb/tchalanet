package com.tchalanet.server.platform.identity.internal.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.platform.identity.api.ProvisionExternalUserRequest;
import org.junit.jupiter.api.Test;

class UnsupportedIdentityProvisioningServiceTest {

  @Test
  void failsClearlyWhenConfiguredProviderDoesNotSupportManagedProvisioning() {
    var service = new UnsupportedIdentityProvisioningService("keycloak");

    assertThatThrownBy(
            () ->
                service.provisionUser(
                    new ProvisionExternalUserRequest(
                        null, "admin@example.test", null, "Admin", null)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not supported")
        .hasMessageContaining("keycloak");
  }
}

