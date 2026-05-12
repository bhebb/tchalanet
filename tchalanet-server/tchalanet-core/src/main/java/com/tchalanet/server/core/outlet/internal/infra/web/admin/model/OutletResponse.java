package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.address.api.model.AddressView;

public record OutletResponse(
    OutletId id,
    TenantId tenantId,
    String name,
    String slug,
    Boolean dayClosed,
    Boolean receiptPrintingEnabled,
    AddressView address) {}
