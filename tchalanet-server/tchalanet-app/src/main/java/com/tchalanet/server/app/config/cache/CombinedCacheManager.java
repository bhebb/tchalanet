package com.tchalanet.server.app.config.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.lang.Nullable;

public class CombinedCacheManager implements CacheManager {

  private final CaffeineCacheManager local;
  @Nullable private final CacheManager remote;
  private final Map<String, Cache> caches = new ConcurrentHashMap<>();

  public CombinedCacheManager(CaffeineCacheManager local, @Nullable CacheManager remote) {
    this.local = local;
    this.remote = remote;
  }

  @Override
  public Cache getCache(String name) {
    return caches.computeIfAbsent(
        name,
        n -> {
          var localCache = local.getCache(n);
          var remoteCache = remote == null ? null : remote.getCache(n);
          return new CombinedCache(n, localCache, remoteCache);
        });
  }

  @Override
  public Collection<String> getCacheNames() {
    if (remote == null) return local.getCacheNames();
    var names = local.getCacheNames();
    var rnames = remote.getCacheNames();
    var set = new java.util.LinkedHashSet<String>();
    set.addAll(names);
    set.addAll(rnames);
    return Collections.unmodifiableCollection(set);
  }
}
