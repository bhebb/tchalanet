package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.internal.domain.model.SalesZone;

import java.util.List;
import java.util.Optional;

public interface SalesZoneReaderPort {

    Optional<SalesZone> findById(TenantId tenantId, SalesZoneId zoneId);

    List<SalesZone> findAllByTenant(TenantId tenantId);

    default SalesZone getRequired(TenantId tenantId, SalesZoneId zoneId) {
        return findById(tenantId, zoneId)
            .orElseThrow(() -> new IllegalArgumentException(
                "SalesZone not found: " + zoneId + " for tenant " + tenantId));
    }
}
