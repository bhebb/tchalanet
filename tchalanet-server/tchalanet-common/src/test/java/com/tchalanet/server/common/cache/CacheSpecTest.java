package com.tchalanet.server.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CacheSpec")
class CacheSpecTest {

  @Nested
  @DisplayName("of(name, ttlL2)")
  class TwoArgFactory {

    @Test
    @DisplayName("should use default L1 when L2 is larger")
    void shouldUseDefaultL1WhenL2IsLarger() {
      var spec = CacheSpec.of("test", Duration.ofMinutes(60));

      assertThat(spec.ttlL1()).isEqualTo(CacheSpec.DEFAULT_TTL_L1);
      assertThat(spec.ttlL2()).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    @DisplayName("should clamp L1 to L2 when L2 is smaller than default L1")
    void shouldClampL1ToL2WhenL2IsSmaller() {
      var spec = CacheSpec.of("test", Duration.ofMinutes(5));

      assertThat(spec.ttlL1()).isEqualTo(Duration.ofMinutes(5));
      assertThat(spec.ttlL2()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("should use default L2 when null")
    void shouldUseDefaultL2WhenNull() {
      var spec = CacheSpec.of("test", (Duration) null);

      assertThat(spec.ttlL2()).isEqualTo(CacheSpec.DEFAULT_TTL_L2);
    }
  }

  @Nested
  @DisplayName("of(name, ttlL1, ttlL2)")
  class ThreeArgFactory {

    @Test
    @DisplayName("should accept explicit L1 and L2")
    void shouldAcceptExplicitValues() {
      var spec = CacheSpec.of("test", Duration.ofMinutes(5), Duration.ofMinutes(30));

      assertThat(spec.ttlL1()).isEqualTo(Duration.ofMinutes(5));
      assertThat(spec.ttlL2()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("should clamp L1 down to L2 when L1 exceeds L2")
    void shouldClampL1DownToL2() {
      var spec = CacheSpec.of("test", Duration.ofMinutes(30), Duration.ofMinutes(10));

      assertThat(spec.ttlL1()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("should use defaults when both null")
    void shouldUseDefaultsWhenBothNull() {
      var spec = CacheSpec.of("test", null, null);

      assertThat(spec.ttlL1()).isEqualTo(CacheSpec.DEFAULT_TTL_L1);
      assertThat(spec.ttlL2()).isEqualTo(CacheSpec.DEFAULT_TTL_L2);
    }
  }
}
