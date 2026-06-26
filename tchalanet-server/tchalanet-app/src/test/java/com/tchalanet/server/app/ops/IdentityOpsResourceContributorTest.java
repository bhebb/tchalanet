package com.tchalanet.server.app.ops;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class IdentityOpsResourceContributorTest {

  @Test
  @DisplayName("reports firebase emulator as ok outside production when host is configured")
  void firebaseEmulatorOkOutsideProduction() {
    var env = new MockEnvironment()
        .withProperty("tch.identity.provider", "firebase-emulator")
        .withProperty("FIREBASE_AUTH_EMULATOR_HOST", "localhost:9099");

    var item = new IdentityOpsResourceContributor(env).services().getFirst();

    assertThat(item.status()).isEqualTo("EMULATOR");
    assertThat(item.severity()).isEqualTo("OK");
  }

  @Test
  @DisplayName("reports firebase emulator as critical in production")
  void firebaseEmulatorCriticalInProduction() {
    var env = new MockEnvironment()
        .withProperty("tch.identity.provider", "firebase-emulator")
        .withProperty("FIREBASE_AUTH_EMULATOR_HOST", "localhost:9099")
        .withProperty("spring.profiles.active", "prod");
    env.setActiveProfiles("prod");

    var item = new IdentityOpsResourceContributor(env).services().getFirst();

    assertThat(item.status()).isEqualTo("INVALID");
    assertThat(item.severity()).isEqualTo("CRITICAL");
  }

  @Test
  @DisplayName("reports firebase live with revocation off as warning")
  void firebaseLiveWarnsWhenRevocationOff() {
    var env = new MockEnvironment()
        .withProperty("tch.identity.provider", "firebase")
        .withProperty("tch.identity.firebase.project-id", "tchalanet-test")
        .withProperty("tch.identity.firebase.revocation-check-mode", "off");

    var item = new IdentityOpsResourceContributor(env).services().getFirst();

    assertThat(item.status()).isEqualTo("LIVE");
    assertThat(item.severity()).isEqualTo("WARNING");
  }
}
