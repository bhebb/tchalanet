package com.tchalanet.server.platform.identity.internal.local;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LocalIdentityPropertiesTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"short", "less-than-thirty-two-characters"})
  void rejectsMissingOrWeakSecret(String secret) {
    assertThatThrownBy(() -> new LocalIdentityProperties("tchalanet-local", secret).requiredSecret())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("32");
  }
}
