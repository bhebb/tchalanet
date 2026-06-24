package com.tchalanet.server.features.ops.cache;

import java.time.Instant;

public record CacheView(
    String cacheName,
    long size,
    Double hitRate,
    Instant lastClearedAt
) {}
