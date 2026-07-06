package com.tchalanet.server.platform.tenant.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;

import java.time.LocalDate;

/**
 * Resolves whether a business day is open for a tenant.
 *
 * <p>Evaluation priority (first match wins):
 * <ol>
 *   <li>business_day_override tenant-level dated exception</li>
 *   <li>TenantBusinessCalendarRules.holidays</li>
 *   <li>TenantBusinessCalendarRules.closedWeekdays</li>
 *   <li>TenantBusinessCalendarRules.defaultOpen</li>
 *   <li>fallback: open</li>
 * </ol>
 *
 * Seller-terminal operational availability is evaluated separately before sale.
 */
public interface TenantBusinessCalendarApi {

    TenantBusinessDayView resolveBusinessDay(
        TenantId tenantId,
        LocalDate businessDate
    );
}
