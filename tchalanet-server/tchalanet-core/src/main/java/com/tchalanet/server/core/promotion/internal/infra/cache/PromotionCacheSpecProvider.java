package com.tchalanet.server.core.promotion.internal.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromotionCacheSpecProvider implements CacheSpecProvider {

    // Use literal names to avoid static coupling during initialization
    public static final String PROMOTION_RUNTIME_ACTIVE = "core.promotion.runtime.active";
    public static final String PROMOTION_CAMPAIGN_BY_ID = "core.promotion.campaign.by_id";
    public static final String PROMOTION_CAMPAIGN_ADMIN_LIST = "core.promotion.campaign.admin_list";

    private static final Duration L1_SHORT = Duration.ofSeconds(60);
    private static final Duration L2_RUNTIME = Duration.ofMinutes(15);
    private static final Duration L2_ADMIN = Duration.ofHours(2);

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            CacheSpec.of(PROMOTION_RUNTIME_ACTIVE, L1_SHORT, L2_RUNTIME),
            CacheSpec.of(PROMOTION_CAMPAIGN_BY_ID, L1_SHORT, L2_ADMIN),
            CacheSpec.of(PROMOTION_CAMPAIGN_ADMIN_LIST, L1_SHORT, L2_ADMIN)
        );
    }
}
