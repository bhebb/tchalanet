package com.tchalanet.server.platform.tenantconfig.internal.service;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantBootstrapView;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantconfig.api.TenantZoneApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
class DefaultTenantZoneApi implements TenantZoneApi {

    private final TenantCatalog tenantCatalog;

    @Override
    public ZoneId resolveTenantZone(TenantId tenantId) {
        return tenantCatalog.findBootstrapById(tenantId)
            .map(TenantBootstrapView::timezone)
            .orElse(ZoneOffset.UTC);
    }
}
