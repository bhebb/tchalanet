package com.tchalanet.server.platform.identity.internal.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class AppUserBootstrapProductionGuardTest {

  @ParameterizedTest
  @ValueSource(strings = {"prod", "production"})
  void rejectsControlledAutoWithoutExplicitProductionApproval(String profile) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(profile);
    var properties =
        new UserBootstrapProperties(
            true, false, AppUserBootstrapMode.CONTROLLED_AUTO, List.of(), List.of(), false);

    assertThatThrownBy(() -> new AppUserBootstrapProductionGuard(properties, environment).validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("controlled-auto");
  }

  @ParameterizedTest
  @ValueSource(strings = {"prod", "production"})
  void allowsControlledAutoWithExplicitApprovalAndAllowlist(String profile) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(profile);
    var properties =
        new UserBootstrapProperties(
            true,
            false,
            AppUserBootstrapMode.CONTROLLED_AUTO,
            List.of("approved@example.com"),
            List.of(),
            true);

    assertThatNoException()
        .isThrownBy(() -> new AppUserBootstrapProductionGuard(properties, environment).validate());
  }
}
