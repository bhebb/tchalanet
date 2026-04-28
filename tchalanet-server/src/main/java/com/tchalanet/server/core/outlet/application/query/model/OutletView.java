package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.core.address.application.model.AddressView;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

public record OutletView(
    OutletId id,
    TenantId tenantId,
    String name,
    String slug,
    Boolean dayClosed,
    Boolean receiptPrintingEnabled,
    AddressView address) {}
