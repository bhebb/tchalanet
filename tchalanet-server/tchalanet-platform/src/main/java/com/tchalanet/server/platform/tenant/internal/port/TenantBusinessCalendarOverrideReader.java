package com.tchalanet.server.platform.tenant.internal.port;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Reads business_day_override rows for a given tenant/outlet/date.
 * Separate from TenantConfigReader to keep the JDBC queries focused.
 */
public interface TenantBusinessCalendarOverrideReader {

    Optional<TenantBusinessDayView> findTenantOverride(
        TenantId tenantId, LocalDate date);
}
