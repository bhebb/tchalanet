package com.tchalanet.server.core.draw.internal.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DrawCacheSpecProvider implements CacheSpecProvider {

    public static final String DRAW_SUMMARY_SEARCH = DrawCacheEvictor.SEARCH;
    public static final String DRAW_TODAY_SEARCH = DrawCacheEvictor.TODAY;
    public static final String DRAW_UPCOMING_SEARCH = DrawCacheEvictor.UPCOMING;
    public static final String DRAW_NEXT_SEARCH = DrawCacheEvictor.NEXT;
    public static final String DRAW_LATEST_RESULTS_SEARCH = DrawCacheEvictor.LATEST;

    private static final Duration L1_SHORT = Duration.ofSeconds(10);
    private static final Duration L2_SHORT = Duration.ofSeconds(60);

    private static final Duration L1_NEXT = Duration.ofSeconds(10);
    private static final Duration L2_NEXT = Duration.ofSeconds(60);

    private static final Duration L1_LATEST = Duration.ofSeconds(10);
    private static final Duration L2_LATEST = Duration.ofSeconds(60);

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            CacheSpec.of(
                DRAW_SUMMARY_SEARCH,
                L1_SHORT,
                L2_SHORT),
            CacheSpec.of(
                DRAW_TODAY_SEARCH,
                L1_SHORT,
                L2_SHORT),
            CacheSpec.of(
                DRAW_UPCOMING_SEARCH,
                L1_SHORT,
                L2_SHORT),
            CacheSpec.of(
                DRAW_NEXT_SEARCH,
                L1_NEXT,
                L2_NEXT),
            CacheSpec.of(
                DRAW_LATEST_RESULTS_SEARCH,
                L1_LATEST,
                L2_LATEST));
    }
}
