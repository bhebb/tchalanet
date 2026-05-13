package com.tchalanet.server.platform.notification.internal.batch;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class BatchNotificationCacheSpecProvider implements CacheSpecProvider {

    public static final String CACHE_NAME = "infra.notification.batch_dedup";

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            CacheSpec.of(
                CACHE_NAME,
                Duration.ofMinutes(30),
                Duration.ofHours(2)
            )
        );
    }
}
