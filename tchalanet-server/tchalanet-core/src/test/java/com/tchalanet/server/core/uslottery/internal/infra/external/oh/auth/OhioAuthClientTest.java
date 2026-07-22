package com.tchalanet.server.core.uslottery.internal.infra.external.oh.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OhioAuthClient")
class OhioAuthClientTest {

  @Test
  @DisplayName("extracts token from wrapped Ohio login response")
  void extractsWrappedToken() {
    var response =
        new OhioAuthClient.OhioLoginResponse(
            null, null, null, new OhioAuthClient.OhioLoginData("wrapped-token", null, null));

    assertThat(response.token()).isEqualTo("wrapped-token");
  }

  @Test
  @DisplayName("keeps support for root token fields")
  void extractsRootToken() {
    var response = new OhioAuthClient.OhioLoginResponse("root-token", null, null, null);

    assertThat(response.token()).isEqualTo("root-token");
  }
}
