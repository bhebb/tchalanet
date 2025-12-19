package com.tchalanet.server.common.cache;

import java.time.Duration;

public record CacheSpec(String name, Duration ttlL1, Duration ttlL2) {

    public static final Duration DEFAULT_TTL_L1 = Duration.ofMinutes(10);
    public static final Duration DEFAULT_TTL_L2 = Duration.ofMinutes(60); // ou 30 si tu préfères

    public static CacheSpec of(String name, Duration ttlL2) {
        var l2 = (ttlL2 != null) ? ttlL2 : DEFAULT_TTL_L2;
        var l1 = DEFAULT_TTL_L1.compareTo(l2) <= 0 ? DEFAULT_TTL_L1 : l2; // enforce L1 <= L2
        return new CacheSpec(name, l1, l2);
    }

    public static CacheSpec of(String name, Duration ttlL1, Duration ttlL2) {
        var l2 = (ttlL2 != null) ? ttlL2 : DEFAULT_TTL_L2;
        var l1 = (ttlL1 != null) ? ttlL1 : DEFAULT_TTL_L1;
        if (l1.compareTo(l2) > 0) l1 = l2;
        return new CacheSpec(name, l1, l2);
    }

}

