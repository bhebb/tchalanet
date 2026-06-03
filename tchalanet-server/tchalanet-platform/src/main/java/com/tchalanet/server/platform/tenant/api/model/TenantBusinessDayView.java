package com.tchalanet.server.platform.tenant.api.model;

import com.tchalanet.server.common.types.id.TenantId;

import java.time.LocalDate;

public record TenantBusinessDayView(
    TenantId tenantId,
    LocalDate businessDate,
    boolean open,
    String reasonCode,
    String label
) {}
