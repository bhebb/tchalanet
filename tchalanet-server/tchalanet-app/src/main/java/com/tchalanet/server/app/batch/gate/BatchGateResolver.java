package com.tchalanet.server.app.batch.gate;

import com.tchalanet.server.app.job.registry.AppBatchJobKeys;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchGateResolver {

    public static final String MASTER_GLOBAL_JOB_DISABLED = "MASTER_GLOBAL_OFF";

    private static final boolean DEFAULT_ENABLED = true;
    private static final String SCOPE_TENANT_OVERRIDE = "TENANT_OVERRIDE";
    private static final String SCOPE_GLOBAL_FLAG = "GLOBAL_FLAG";
    private static final String SCOPE_DEFAULT = "DEFAULT";

    private final BatchGateCache cache;

    public record Resolution(boolean enabled, String scope) {}

    public boolean resolve(JobKey jobKey, TenantId tenantId) {
        return resolveWithScope(jobKey, tenantId).enabled();
    }

    public Resolution resolveWithScope(JobKey jobKey, TenantId tenantId) {
        var master = cache.getGlobal(AppBatchJobKeys.BATCH_GLOBAL_ENABLED);
        if (master.isPresent() && !master.get()) {
            log.debug("batch.gate master disabled jobKey={}", jobKey);
            return new Resolution(false, MASTER_GLOBAL_JOB_DISABLED);
        }

        if (tenantId != null) {
            var tenantOverride = cache.getTenant(jobKey, tenantId);
            if (tenantOverride.isPresent()) {
                boolean enabled = tenantOverride.get();

                log.debug(
                    "batch.gate tenant override jobKey={} tenantId={} enabled={}",
                    jobKey,
                    tenantId,
                    enabled
                );

                return new Resolution(enabled, SCOPE_TENANT_OVERRIDE);
            }
        }

        var globalFlag = cache.getGlobal(jobKey);
        if (globalFlag.isPresent()) {
            boolean enabled = globalFlag.get();

            log.debug("batch.gate global flag jobKey={} enabled={}", jobKey, enabled);

            return new Resolution(enabled, SCOPE_GLOBAL_FLAG);
        }

        log.debug("batch.gate default jobKey={} enabled={}", jobKey, DEFAULT_ENABLED);
        return new Resolution(DEFAULT_ENABLED, SCOPE_DEFAULT);
    }
}
