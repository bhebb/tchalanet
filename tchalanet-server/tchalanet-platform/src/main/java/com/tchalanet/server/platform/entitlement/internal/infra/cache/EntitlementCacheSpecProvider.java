package com.tchalanet.server.platform.entitlement.internal.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class EntitlementCacheSpecProvider implements CacheSpecProvider {

    public static final String TENANT_SNAPSHOT_CACHE = "platform.entitlement.tenant_snapshot";

    private static final Duration L1_TTL = Duration.ofMinutes(5);
    private static final Duration L2_TTL = Duration.ofHours(6);

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            CacheSpec.of(TENANT_SNAPSHOT_CACHE, L1_TTL, L2_TTL)
        );
    }
}
