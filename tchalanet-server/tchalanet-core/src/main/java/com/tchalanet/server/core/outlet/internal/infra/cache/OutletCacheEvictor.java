package com.tchalanet.server.core.outlet.internal.infra.cache;

import com.tchalanet.server.common.types.id.OutletId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutletCacheEvictor {

    private final CacheManager cacheManager;

    public void evictOutlet(OutletId outletId) {
        evict(OutletCacheConfig.OUTLET_BY_ID, outletId.value());
        evict(OutletCacheConfig.OUTLET_OPERATIONAL_CONTEXT, outletId.value());
        evict(OutletCacheConfig.OUTLET_SALES_CAPABILITY, outletId.value());

        clear(OutletCacheConfig.OUTLET_TREE);
    }

    public void evictOutletRelations(OutletId outletId) {
        evict(OutletCacheConfig.OUTLET_OPERATIONAL_CONTEXT, outletId.value());
        clear(OutletCacheConfig.OUTLET_TREE);
    }

    public void evictSalesCapability(OutletId outletId) {
        evict(OutletCacheConfig.OUTLET_BY_ID, outletId.value());
        evict(OutletCacheConfig.OUTLET_OPERATIONAL_CONTEXT, outletId.value());
        evict(OutletCacheConfig.OUTLET_SALES_CAPABILITY, outletId.value());
        clear(OutletCacheConfig.OUTLET_TREE);
    }

    public void evictOutletLists() {
        clear(OutletCacheConfig.OUTLET_TREE);
    }

    private void evict(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clear(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
