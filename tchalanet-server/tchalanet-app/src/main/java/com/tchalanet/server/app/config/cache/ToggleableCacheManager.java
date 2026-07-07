package com.tchalanet.server.app.config.cache;

import com.tchalanet.server.common.cache.CacheToggle;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * Wraps the primary {@link CacheManager} (Combined L1+L2, or Caffeine-only) so every cache honours
 * the runtime {@link CacheToggle}. Transparent when nothing is disabled.
 */
public class ToggleableCacheManager implements CacheManager {

  private final CacheManager delegate;
  private final CacheToggle toggle;
  private final Map<String, Cache> caches = new ConcurrentHashMap<>();

  public ToggleableCacheManager(CacheManager delegate, CacheToggle toggle) {
    this.delegate = delegate;
    this.toggle = toggle;
  }

  @Override
  @Nullable
  public Cache getCache(String name) {
    Cache existing = caches.get(name);
    if (existing != null) {
      return existing;
    }
    Cache underlying = delegate.getCache(name);
    if (underlying == null) {
      return null;
    }
    return caches.computeIfAbsent(name, n -> new ToggleableCache(underlying, toggle));
  }

  @Override
  public Collection<String> getCacheNames() {
    return delegate.getCacheNames();
  }
}
