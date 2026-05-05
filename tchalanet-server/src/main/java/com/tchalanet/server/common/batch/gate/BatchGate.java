package com.tchalanet.server.common.batch.gate;

import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchGate {

    private final BatchGateResolver resolver;

    public boolean enabled(JobKey jobKey, TenantId tenantId) {
        if (jobKey == null) throw new IllegalArgumentException("jobKey required");
        return resolver.resolve(jobKey, tenantId);
    }

    public void assertEnabledOrThrow(JobKey jobKey, TenantId tenantId) {
        if (jobKey == null) {
            throw new IllegalArgumentException("jobKey required");
        }

        var res = resolver.resolveWithScope(jobKey, tenantId);
        if (!res.enabled()) {
            log.warn("batch.disabled jobKey={} tenantId={} source={}", jobKey, tenantId, res.scope());
            throw new BatchDisabledException(jobKey, tenantId, res.scope());
        }
    }
}
