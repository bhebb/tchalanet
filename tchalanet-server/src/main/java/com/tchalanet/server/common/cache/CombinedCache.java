package com.tchalanet.server.common.cache;

import java.util.Objects;
import java.util.concurrent.Callable;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;

public class CombinedCache implements Cache {
    private final String name;
    private final Cache local;
    @Nullable private final Cache remote;

    public CombinedCache(String name, Cache local, @Nullable Cache remote) {
        this.name = Objects.requireNonNull(name);
        this.local = Objects.requireNonNull(local);
        this.remote = remote;
    }

    @Override public String getName() { return name; }

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
            ValueWrapper rv = remote.get(key);
            if (rv != null) {
                local.put(key, rv.get()); // hydrate L1
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
            put(key, val);
            return val;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        if (remote != null) remote.put(key, value);
        local.put(key, value);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) return existing;
        put(key, value);
        return new SimpleValueWrapper(value);
    }

    @Override
    public void evict(Object key) {
        if (remote != null) remote.evict(key);
        local.evict(key);
    }

    @Override
    public void clear() {
        if (remote != null) remote.clear();
        local.clear();
    }
}
