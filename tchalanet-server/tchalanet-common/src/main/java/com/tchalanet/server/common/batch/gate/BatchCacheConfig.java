package com.tchalanet.server.common.batch.gate;

import java.time.Duration;
import java.util.List;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import org.springframework.stereotype.Component;

import static com.tchalanet.server.common.batch.gate.BatchFlagCache.CACHE_NAME;

/**
 * Cache configuration for batch domain.
 * Declares TTL for batch-related caches.
 */
@Component
public class BatchCacheConfig implements CacheSpecProvider {

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            CacheSpec.of(
                CACHE_NAME,
                Duration.ofSeconds(15), // L1 (Caffeine): 15 sec
                Duration.ofMinutes(2)   // L2 (Redis): 2 min
            )
        );
    }
}
