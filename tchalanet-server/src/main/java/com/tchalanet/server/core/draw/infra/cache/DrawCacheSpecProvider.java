package com.tchalanet.server.core.draw.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class DrawCacheSpecProvider implements CacheSpecProvider {

    public static final String DRAW_SUMMARY_BY_TENANT = "core.draw.summary.by_tenant";
    public static final String DRAW_TODAY_BY_TENANT = "core.draw.today.by_tenant";
    public static final String DRAW_NEXT_BY_TENANT = "core.draw.next.by_tenant";

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            CacheSpec.of(
                DRAW_SUMMARY_BY_TENANT,
                Duration.ofSeconds(10), // L1
                Duration.ofSeconds(60)  // L2
            ),
            CacheSpec.of(
                DRAW_TODAY_BY_TENANT,
                Duration.ofSeconds(10), // L1
                Duration.ofSeconds(60)  // L2
            ),
            CacheSpec.of(
                DRAW_NEXT_BY_TENANT,
                Duration.ofSeconds(10), // L1
                Duration.ofSeconds(60)  // L2
            )
        );
    }
}
