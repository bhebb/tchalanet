package com.tchalanet.server.common.cache;

import java.util.Objects;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

public class CombinedCache implements Cache {
  private final String name;
  private final Cache local;
  private final Cache remote; // can be null

  public CombinedCache(String name, Cache local, @Nullable Cache remote) {
    this.name = Objects.requireNonNull(name);
    this.local = Objects.requireNonNull(local);
    this.remote = remote;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Object getNativeCache() {
    // Return an array with both native caches when available
    if (remote == null) return local.getNativeCache();
    return new Object[] {local.getNativeCache(), remote.getNativeCache()};
  }

  @Override
  @Nullable
  public ValueWrapper get(Object key) {
    // 1. try local
    ValueWrapper v = local.get(key);
    if (v != null) return v;

    // 2. try remote
    if (remote != null) {
      ValueWrapper rv = remote.get(key);
      if (rv != null) {
        // propagate to local
        local.put(key, rv.get());
        return rv;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public <T> T get(Object key, Class<T> type) {
    ValueWrapper v = get(key);
    if (v == null) return null;
    Object val = v.get();
    if (type != null && !type.isInstance(val)) return null;
    return (T) val;
  }

  @Override
  @Nullable
  public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
    ValueWrapper v = get(key);
    if (v != null) return (T) v.get();
    try {
      T val = valueLoader.call();
      put(key, val);
      return val;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void put(Object key, @Nullable Object value) {
    local.put(key, value);
    if (remote != null) remote.put(key, value);
  }

  @Override
  @Nullable
  public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
    synchronized (this) {
      ValueWrapper existing = get(key);
      if (existing != null) return existing;
      put(key, value);
      return null;
    }
  }

  @Override
  public void evict(Object key) {
    local.evict(key);
    if (remote != null) remote.evict(key);
  }

  @Override
  public void clear() {
    local.clear();
    if (remote != null) remote.clear();
  }
}
