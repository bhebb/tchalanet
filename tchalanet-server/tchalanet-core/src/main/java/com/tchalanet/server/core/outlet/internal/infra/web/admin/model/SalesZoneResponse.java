package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;

public record SalesZoneResponse(
    SalesZoneId id,
    TenantId tenantId,
    String code,
    String label,
    boolean active,
    SalesZoneId parentId) {}
