package com.tchalanet.server.core.session.internal.application.service.time;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.session.internal.application.port.out.OutletOperationalSettingsReaderPort;
import com.tchalanet.server.platform.tenant.api.TenantZoneApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
class DefaultSalesSessionBusinessDateResolver implements SalesSessionBusinessDateResolver {

    private final OutletOperationalSettingsReaderPort outletSettingsReader;
    private final TenantZoneApi tenantZoneApi;

    @Override
    public LocalDate resolve(TenantId tenantId, OutletId outletId, Instant instant) {
        var zone = outletSettingsReader.findOutletZone(tenantId, outletId)
            .orElseGet(() -> tenantZoneApi.resolveTenantZone(tenantId));
        return instant.atZone(zone).toLocalDate();
    }
}
