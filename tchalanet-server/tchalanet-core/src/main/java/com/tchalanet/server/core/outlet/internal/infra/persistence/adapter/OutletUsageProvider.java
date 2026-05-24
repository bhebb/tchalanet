package com.tchalanet.server.core.outlet.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.platform.entitlement.api.UsageKeys;
import com.tchalanet.server.platform.entitlement.api.UsageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutletUsageProvider implements UsageProvider {

    private final OutletReaderPort outletReaderPort;

    @Override
    public boolean supports(String usageKey) {
        return UsageKeys.OUTLETS_ACTIVE.equals(usageKey);
    }

    @Override
    public int currentUsage(TenantId tenantId, String usageKey) {
        if (!supports(usageKey)) {
            throw new IllegalArgumentException("Unsupported usageKey: " + usageKey);
        }

        return outletReaderPort.countActiveByTenant(tenantId);
    }
}
