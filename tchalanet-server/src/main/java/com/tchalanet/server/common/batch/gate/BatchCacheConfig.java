package com.tchalanet.server.common.batch.gate;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

import static com.tchalanet.server.common.batch.gate.BatchFlagCache.CACHE_NAME;

/**
 * Cache configuration for batch domain.
 * Declares TTL for batch-related caches.
 */
@Configuration
public class BatchCacheConfig {

    @Bean
    public CacheSpecProvider batchCacheSpecProvider() {
        return () -> List.of(
            CacheSpec.of(
                CACHE_NAME,
                Duration.ofMinutes(2),  // L2 (Redis): 2 min
                Duration.ofSeconds(15)  // L1 (Caffeine): 15 sec
            )
        );
    }
}
