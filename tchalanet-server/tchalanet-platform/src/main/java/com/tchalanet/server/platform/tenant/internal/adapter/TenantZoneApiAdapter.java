package com.tchalanet.server.platform.tenant.internal.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.TenantZoneApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class TenantZoneApiAdapter implements TenantZoneApi {

    private final TenantPreContextLookupApi registry;

    @Override
    public ZoneId resolveTenantZone(TenantId tenantId) {
        return registry.findById(tenantId)
            .map(v -> v.timezone())
            .orElse(ZoneOffset.UTC);
    }
}
