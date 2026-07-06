package com.tchalanet.server.platform.tenant.internal.port;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;

import java.time.LocalDate;
import java.util.Optional;

/** Reads active tenant-level business_day_override rows. */
public interface TenantBusinessCalendarOverrideReader {

    Optional<TenantBusinessDayView> findTenantOverride(
        TenantId tenantId, LocalDate date);
}
