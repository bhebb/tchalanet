package com.tchalanet.server.app.config.cache;

import java.util.Objects;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

@Slf4j
public class CombinedCache implements Cache {
  private final String name;
  private final Cache local;
  @Nullable private final Cache remote;

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
    if (remote == null) return local.getNativeCache();
    return new Object[] {local.getNativeCache(), remote.getNativeCache()};
  }

  @Override
  @Nullable
  public ValueWrapper get(Object key) {
    ValueWrapper v = local.get(key);
    if (v != null) return v;

    if (remote != null) {
      ValueWrapper rv = remoteGet(key);
      if (rv != null) {
        local.put(key, rv.get());
        return rv;
      }
    }
    return null;
  }

  @Override
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T get(Object key, @Nullable Class<T> type) {
    ValueWrapper v = get(key);
    if (v == null) return null;
    Object val = v.get();
    if (type != null && !type.isInstance(val)) return null;
    return (T) val;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(Object key, Callable<T> valueLoader) {
    ValueWrapper v = get(key);
    if (v != null) return (T) v.get();
    try {
      T val = valueLoader.call();
      if (val == null) return null;
      put(key, val);
      return val;
    } catch (Exception e) {
      throw new ValueRetrievalException(key, valueLoader, e);
    }
  }

  @Override
  public void put(Object key, @Nullable Object value) {
    if (value == null) {
      evict(key);
      return;
    }
    if (remote != null) remotePut(key, value);
    local.put(key, value);
  }

  @Override
  @Nullable
  public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
    ValueWrapper existing = get(key);
    if (existing != null) return existing;
    put(key, value);
    return null;
  }

  @Override
  public void evict(Object key) {
    if (remote != null) remoteEvict(key);
    local.evict(key);
  }

  @Override
  public void clear() {
    if (remote != null) remoteClear();
    local.clear();
  }

  @Nullable
  private ValueWrapper remoteGet(Object key) {
    try {
      return remote == null ? null : remote.get(key);
    } catch (RuntimeException ex) {
      log.warn("Remote cache get failed cache={} key={} cause={}", name, key, ex.toString());
      remoteEvict(key);
      return null;
    }
  }

  private void remotePut(Object key, Object value) {
    try {
      remote.put(key, value);
    } catch (RuntimeException ex) {
      log.warn("Remote cache put failed cache={} key={} cause={}", name, key, ex.toString());
    }
  }

  private void remoteEvict(Object key) {
    try {
      remote.evict(key);
    } catch (RuntimeException ex) {
      log.warn("Remote cache evict failed cache={} key={} cause={}", name, key, ex.toString());
    }
  }

  private void remoteClear() {
    try {
      remote.clear();
    } catch (RuntimeException ex) {
      log.warn("Remote cache clear failed cache={} cause={}", name, ex.toString());
    }
  }
}
