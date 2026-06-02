package com.tchalanet.server.platform.tenant.api.model;

import com.tchalanet.server.common.types.id.BusinessDayOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

/** Read view of a TENANT-LEVEL business-day override (outlet_id IS NULL). */
public record BusinessDayOverrideView(
    BusinessDayOverrideId id,
    TenantId tenantId,
    LocalDate businessDate,
    boolean open,
    String reasonCode,
    String label) {}
