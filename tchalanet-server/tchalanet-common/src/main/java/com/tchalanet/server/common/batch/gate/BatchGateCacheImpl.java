package com.tchalanet.server.common.batch.gate;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BatchGateCacheImpl implements BatchGateCache {

    private static final String NAMESPACE = "batch";

    private final BatchFlagCache flagCache;
    private final BatchGateFlagStore flagStore;

    @Override
    public Optional<Boolean> getTenant(JobKey jobKey, TenantId tenantId) {
        if (tenantId == null || jobKey == null) {
            return Optional.empty();
        }
        String cacheKey = NAMESPACE + ":t:" + tenantId.value() + ":" + jobKey.value();
        return Optional.ofNullable(
            flagCache.getBool(cacheKey, () -> loadTenantFlag(tenantId, jobKey))
        );
    }

    @Override
    public Optional<Boolean> getGlobal(JobKey jobKey) {
        if (jobKey == null) {
            return Optional.empty();
        }
        String cacheKey = NAMESPACE + ":g:" + jobKey.value();
        return Optional.ofNullable(
            flagCache.getBool(cacheKey, () -> loadGlobalFlag(jobKey))
        );
    }

    @Override
    public void cacheTenant(JobKey jobKey, TenantId tenantId, boolean enabled) {
        // cache only
        if (jobKey != null && tenantId != null) {
            String cacheKey = NAMESPACE + ":t:" + tenantId.value() + ":" + jobKey.value();
            flagCache.put(cacheKey, enabled);
        }
    }

    @Override
    public void cacheGlobal(JobKey jobKey, boolean enabled) {
        // cache only
        if (jobKey != null) {
            String cacheKey = NAMESPACE + ":g:" + jobKey.value();
            flagCache.put(cacheKey, enabled);
        }
    }

    @Override
    public void evictTenant(JobKey jobKey, TenantId tenantId) {
        if (tenantId != null && jobKey != null) {
            flagCache.evict(NAMESPACE + ":t:" + tenantId.value() + ":" + jobKey.value());
        }
    }

    @Override
    public void evictGlobal(JobKey jobKey) {
        if (jobKey != null) {
            flagCache.evict(NAMESPACE + ":g:" + jobKey.value());
        }
    }

    @Override
    public void evictAll() {
        flagCache.clear();
    }

    private Boolean loadTenantFlag(TenantId tenantId, JobKey jobKey) {
        return flagStore.findTenantFlag(jobKey, tenantId).orElse(null);
    }

    private Boolean loadGlobalFlag(JobKey jobKey) {
        return flagStore.findGlobalFlag(jobKey).orElse(null);
    }
}
