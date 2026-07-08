package com.tchalanet.server.common.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Runtime kill-switch for individual caches.
 *
 * <p>Disabling a cache makes it behave as a no-op (reads miss, writes are dropped) without a
 * redeploy — the escape hatch when a cache starts returning bad data. Eviction/clear still work on a
 * disabled cache.
 *
 * <p>State is in-memory and per-instance: an operator must toggle each running instance (or this can
 * be backed by a shared store / settings later). It is not a functional mechanism — see cache
 * policy §11.
 */
@Component
public class CacheToggle {

  private final Set<String> disabled = ConcurrentHashMap.newKeySet();

  /** @return true if the cache is active (default), false if disabled. */
  public boolean isEnabled(String cacheName) {
    return !disabled.contains(cacheName);
  }

  public void disable(String cacheName) {
    disabled.add(cacheName);
  }

  public void enable(String cacheName) {
    disabled.remove(cacheName);
  }

  public Set<String> disabledCaches() {
    return Set.copyOf(disabled);
  }
}
