package com.tchalanet.server.app.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.cache.CacheToggle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.support.SimpleValueWrapper;

@DisplayName("ToggleableCache")
class ToggleableCacheTest {

  private Cache delegate;
  private CacheToggle toggle;
  private ToggleableCache cache;

  @BeforeEach
  void setUp() {
    delegate = mock(Cache.class);
    when(delegate.getName()).thenReturn("test");
    toggle = new CacheToggle();
    cache = new ToggleableCache(delegate, toggle);
  }

  @Nested
  @DisplayName("when enabled")
  class Enabled {

    @Test
    @DisplayName("get should delegate")
    void shouldDelegateGet() {
      ValueWrapper vw = new SimpleValueWrapper("val");
      when(delegate.get("k")).thenReturn(vw);

      assertThat(cache.get("k")).isSameAs(vw);
    }

    @Test
    @DisplayName("put should delegate")
    void shouldDelegatePut() {
      cache.put("k", "v");
      verify(delegate).put("k", "v");
    }
  }

  @Nested
  @DisplayName("when disabled")
  class Disabled {

    @BeforeEach
    void disable() {
      toggle.disable("test");
    }

    @Test
    @DisplayName("get should return null (miss)")
    void shouldReturnNull() {
      assertThat(cache.get("k")).isNull();
      verify(delegate, never()).get("k");
    }

    @Test
    @DisplayName("put should be dropped")
    void shouldDropPut() {
      cache.put("k", "v");
      verify(delegate, never()).put("k", "v");
    }

    @Test
    @DisplayName("evict should always delegate")
    void shouldAlwaysDelegateEvict() {
      cache.evict("k");
      verify(delegate).evict("k");
    }

    @Test
    @DisplayName("clear should always delegate")
    void shouldAlwaysDelegateClear() {
      cache.clear();
      verify(delegate).clear();
    }
  }
}
