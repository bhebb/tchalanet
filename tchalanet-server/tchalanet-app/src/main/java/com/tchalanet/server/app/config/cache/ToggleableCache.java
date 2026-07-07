package com.tchalanet.server.app.config.cache;

import com.tchalanet.server.common.cache.CacheToggle;
import java.util.concurrent.Callable;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

/**
 * Wraps a cache with the runtime {@link CacheToggle}. When the cache is disabled it behaves as a
 * no-op: reads miss (so {@code @Cacheable} recomputes from the source), writes are dropped. Eviction
 * and clear always delegate, so a disabled cache can still be flushed.
 */
public class ToggleableCache implements Cache {

  private final Cache delegate;
  private final CacheToggle toggle;

  public ToggleableCache(Cache delegate, CacheToggle toggle) {
    this.delegate = delegate;
    this.toggle = toggle;
  }

  private boolean enabled() {
    return toggle.isEnabled(delegate.getName());
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public Object getNativeCache() {
    return delegate.getNativeCache();
  }

  @Override
  @Nullable
  public ValueWrapper get(Object key) {
    return enabled() ? delegate.get(key) : null;
  }

  @Override
  @Nullable
  public <T> T get(Object key, @Nullable Class<T> type) {
    return enabled() ? delegate.get(key, type) : null;
  }

  @Override
  @Nullable
  public <T> T get(Object key, Callable<T> valueLoader) {
    if (enabled()) {
      return delegate.get(key, valueLoader);
    }
    try {
      return valueLoader.call();
    } catch (Exception e) {
      throw new ValueRetrievalException(key, valueLoader, e);
    }
  }

  @Override
  public void put(Object key, @Nullable Object value) {
    if (enabled()) {
      delegate.put(key, value);
    }
  }

  @Override
  @Nullable
  public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
    return enabled() ? delegate.putIfAbsent(key, value) : null;
  }

  @Override
  public void evict(Object key) {
    delegate.evict(key);
  }

  @Override
  public boolean evictIfPresent(Object key) {
    return delegate.evictIfPresent(key);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public boolean invalidate() {
    return delegate.invalidate();
  }
}
