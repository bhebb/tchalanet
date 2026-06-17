package com.tchalanet.server.platform.identity.internal.firebase;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;

class FirebaseEmulatorProductionGuardTest {

  @ParameterizedTest
  @CsvSource({"prod", "production", "prod-ca"})
  void rejectsEmulatorInProduction(String profile) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(profile);

    assertThatThrownBy(
            () -> new FirebaseEmulatorProductionGuard("firebase-emulator", environment).validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("forbidden");
  }

  @ParameterizedTest
  @CsvSource({"firebase-emulator, local-ide", "firebase, prod", "local-jwt, perf"})
  void allowsExpectedProviderProfileCombinations(String provider, String profile) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(profile);

    assertThatNoException()
        .isThrownBy(() -> new FirebaseEmulatorProductionGuard(provider, environment).validate());
  }

  @ParameterizedTest
  @CsvSource({"firebase, ''", "firebase-emulator, localhost:9099"})
  void acceptsValidFirebaseAdminEndpointConfiguration(String provider, String emulatorHost) {
    assertThatNoException()
        .isThrownBy(() -> FirebaseAdminConfig.validateEmulatorHost(provider, emulatorHost));
  }

  @ParameterizedTest
  @CsvSource({"''", "' '"})
  void rejectsEmulatorWithoutItsHost(String emulatorHost) {
    assertThatThrownBy(
            () -> FirebaseAdminConfig.validateEmulatorHost("firebase-emulator", emulatorHost))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("FIREBASE_AUTH_EMULATOR_HOST");
  }
}
