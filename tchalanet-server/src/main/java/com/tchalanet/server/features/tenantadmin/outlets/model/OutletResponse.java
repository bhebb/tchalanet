package com.tchalanet.server.features.tenantadmin.outlets.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

public record OutletResponse(
    OutletId id,
    TenantId tenantId,
    String name,
    String slug,
    Boolean dayClosed,
    Boolean receiptPrintingEnabled
) {}
