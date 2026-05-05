package com.tchalanet.server.common.batch.notification;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

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
