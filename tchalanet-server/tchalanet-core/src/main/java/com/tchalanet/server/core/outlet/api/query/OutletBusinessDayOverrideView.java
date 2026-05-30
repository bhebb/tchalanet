package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.types.id.BusinessDayOverrideId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

/** Read view of an OUTLET-LEVEL business-day override. */
public record OutletBusinessDayOverrideView(
    BusinessDayOverrideId id,
    TenantId tenantId,
    OutletId outletId,
    LocalDate businessDate,
    boolean open,
    String reasonCode,
    String label) {}
