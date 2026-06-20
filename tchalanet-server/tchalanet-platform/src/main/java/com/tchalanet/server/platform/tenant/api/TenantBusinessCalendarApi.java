package com.tchalanet.server.platform.tenant.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;

import java.time.LocalDate;

/**
 * Resolves whether a business day is open for a given tenant/outlet context.
 *
 * <p>Evaluation priority (first match wins):
 * <ol>
 *   <li>business_day_override outlet-level (if outletId provided)</li>
 *   <li>business_day_override tenant-level</li>
 *   <li>TenantBusinessCalendarRules.closedWeekdays</li>
 *   <li>TenantBusinessCalendarRules.defaultOpen</li>
 *   <li>fallback: open</li>
 * </ol>
 *
 * Note: {@code outlet.day_closed} is an immediate operational flag evaluated
 * separately (in the SQL opening context query), before this API is called.
 */
public interface TenantBusinessCalendarApi {

    TenantBusinessDayView resolveBusinessDay(
        TenantId tenantId,
        LocalDate businessDate
    );
}
