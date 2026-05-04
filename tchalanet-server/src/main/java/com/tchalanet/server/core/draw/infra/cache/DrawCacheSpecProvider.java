package com.tchalanet.server.core.draw.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class DrawCacheSpecProvider implements CacheSpecProvider {

    public static final String DRAW_SUMMARY_SEARCH = "core.draw.summary.search";
    public static final String DRAW_TODAY_SEARCH = "core.draw.today.search";
    public static final String DRAW_UPCOMING_SEARCH = "core.draw.upcoming.search";

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            CacheSpec.of(
                DRAW_SUMMARY_SEARCH,
                Duration.ofSeconds(10), // L1
                Duration.ofSeconds(60)  // L2
            ),
            CacheSpec.of(
                DRAW_TODAY_SEARCH,
                Duration.ofSeconds(10), // L1
                Duration.ofSeconds(60)  // L2
            ),
            CacheSpec.of(
                DRAW_UPCOMING_SEARCH,
                Duration.ofSeconds(10), // L1
                Duration.ofSeconds(60)  // L2
            )
        );
    }
}
