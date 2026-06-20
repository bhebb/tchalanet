package com.tchalanet.server.platform.identity.internal.local;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;

class LocalIdentityProductionGuardTest {

  @ParameterizedTest
  @CsvSource({
    "local-jwt, prod",
    "local-jwt, production",
    "local-jwt, prod-ca",
    "local-perf, prod",
    "local-perf, production",
    "local-perf, production-east",
    "keycloak, production"
  })
  void rejectsForbiddenProviderInProduction(String provider, String profile) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(profile);

    assertThatThrownBy(() -> new LocalIdentityProductionGuard(provider, environment).validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("forbidden");
  }

  @ParameterizedTest
  @CsvSource({"local-jwt, dev", "local-perf, perf", "firebase, prod", "keycloak, dev"})
  void allowsExpectedProviderProfileCombinations(String provider, String profile) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(profile);

    assertThatNoException()
        .isThrownBy(() -> new LocalIdentityProductionGuard(provider, environment).validate());
  }
}
