package com.tchalanet.server.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CacheToggle")
class CacheToggleTest {

    @Test
    @DisplayName("should be enabled by default")
    void shouldBeEnabledByDefault() {
        var toggle = new CacheToggle();
        assertThat(toggle.isEnabled("myCache")).isTrue();
    }

    @Test
    @DisplayName("should disable a cache")
    void shouldDisableCache() {
        var toggle = new CacheToggle();
        toggle.disable("myCache");

        assertThat(toggle.isEnabled("myCache")).isFalse();
        assertThat(toggle.disabledCaches()).containsExactly("myCache");
    }

    @Test
    @DisplayName("should re-enable a disabled cache")
    void shouldReEnableCache() {
        var toggle = new CacheToggle();
        toggle.disable("myCache");
        toggle.enable("myCache");

        assertThat(toggle.isEnabled("myCache")).isTrue();
        assertThat(toggle.disabledCaches()).isEmpty();
    }

    @Test
    @DisplayName("should isolate caches by name")
    void shouldIsolateCachesByName() {
        var toggle = new CacheToggle();
        toggle.disable("cacheA");

        assertThat(toggle.isEnabled("cacheA")).isFalse();
        assertThat(toggle.isEnabled("cacheB")).isTrue();
    }

    @Test
    @DisplayName("disabledCaches should return immutable copy")
    void shouldReturnImmutableCopy() {
        var toggle = new CacheToggle();
        toggle.disable("x");

        var snapshot = toggle.disabledCaches();
        toggle.enable("x");

        assertThat(snapshot).containsExactly("x");
        assertThat(toggle.disabledCaches()).isEmpty();
    }
}
