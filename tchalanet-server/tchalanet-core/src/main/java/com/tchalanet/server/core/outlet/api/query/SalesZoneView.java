package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;

public record SalesZoneView(
    SalesZoneId id,
    TenantId tenantId,
    String code,
    String label,
    boolean active,
    SalesZoneId parentId) {}
