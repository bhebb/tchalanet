package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletKind;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import com.tchalanet.server.platform.address.api.model.AddressView;

public record OutletResponse(
    OutletId id,
    TenantId tenantId,
    String name,
    String slug,
    OutletKind kind,
    String partnerRef,
    SalesZoneId zoneId,
    OutletStatus status,
    Boolean dayClosed,
    Boolean outletBlocked,
    String outletBlockReason,
    Boolean receiptPrintingEnabled,
    AddressView address) {}
