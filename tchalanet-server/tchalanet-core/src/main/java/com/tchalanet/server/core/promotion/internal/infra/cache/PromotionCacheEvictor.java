package com.tchalanet.server.core.promotion.internal.infra.cache;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCacheEvictorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class PromotionCacheEvictor implements PromotionCacheEvictorPort {

    public static final String RUNTIME_ACTIVE = "core.promotion.runtime.active";
    public static final String CAMPAIGN_BY_ID = "core.promotion.campaign.by_id";
    public static final String CAMPAIGN_ADMIN_LIST = "core.promotion.campaign.admin_list";

    private final CacheManager cacheManager;

    @Override
    public void evictAfterCampaignMutation(
        TenantId tenantId,
        PromotionCampaignId campaignId
    ) {
        evictRuntimeForTenant(tenantId);
        evictCampaignDetail(tenantId, campaignId);
        clearAdminLists();
    }

    @Override
    public void evictRuntimeForTenant(TenantId tenantId) {
        evictKey(RUNTIME_ACTIVE, tenantKey(tenantId));
    }

    @Override
    public void evictCampaignDetail(
        TenantId tenantId,
        PromotionCampaignId campaignId
    ) {
        evictKey(CAMPAIGN_BY_ID, campaignKey(tenantId, campaignId));
    }

    @Override
    public void clearAdminLists() {
        clear(CAMPAIGN_ADMIN_LIST);
    }

    static String tenantKey(TenantId tenantId) {
        return tenantId.value().toString();
    }

    static String campaignKey(
        TenantId tenantId,
        PromotionCampaignId campaignId
    ) {
        return tenantId.value() + ":" + campaignId.value();
    }

    private void evictKey(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            log.debug("promotion.cache not configured cacheName={}", cacheName);
            return;
        }

        cache.evict(key);

        log.debug("promotion.cache evicted cacheName={} key={}", cacheName, key);
    }

    private void clear(String cacheName) {
        var cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            log.debug("promotion.cache not configured cacheName={}", cacheName);
            return;
        }

        cache.clear();

        log.debug("promotion.cache cleared cacheName={}", cacheName);
    }
}
