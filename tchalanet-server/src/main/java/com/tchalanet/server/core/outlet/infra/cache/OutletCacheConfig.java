package com.tchalanet.server.core.outlet.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OutletCacheConfig implements CacheSpecProvider {

    public static final String OUTLET_BY_ID = "tenant.outlet.by_id";
    public static final String OUTLET_OPERATIONAL_CONTEXT = "tenant.outlet.operational_context";
    public static final String OUTLET_SALES_CAPABILITY = "tenant.outlet.sales_capability";
    public static final String OUTLET_TREE = "tenant.outlet.tree";

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            // Outlet details/config: changes occasionally, safe medium TTL.
            CacheSpec.of(OUTLET_BY_ID, Duration.ofMinutes(20)),

            // Runtime read model used by Sales/Payout/Session decisions.
            CacheSpec.of(OUTLET_OPERATIONAL_CONTEXT, Duration.ofMinutes(10)),

            // Sales capability can change after block/unblock/day close/session state.
            CacheSpec.of(OUTLET_SALES_CAPABILITY, Duration.ofMinutes(5)),

            // Outlet + terminal hierarchy for admin/dashboard UI.
            CacheSpec.of(OUTLET_TREE, Duration.ofMinutes(10)));
    }
}
