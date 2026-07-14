package com.tchalanet.server.platform.tenanttheme.internal.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TenantThemeTokenValidatorTest {

  private final TenantThemeTokenValidator validator = new TenantThemeTokenValidator();

  @Test
  void nullOverridesAreAllowed() {
    assertThatNoException().isThrownBy(() -> validator.validateOverrides(null));
  }

  @Test
  void emptyOverridesAreAllowed() {
    assertThatNoException().isThrownBy(() -> validator.validateOverrides(Map.of()));
  }

  @Test
  void nonEmptyOverridesAreRejectedInV1() {
    assertThatThrownBy(() -> validator.validateOverrides(Map.of("color.primary", "#FF0000")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("V1");
  }
}
