package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.TenantBusinessCalendarApi;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantBusinessCalendarRules;
import com.tchalanet.server.platform.tenant.internal.port.TenantBusinessCalendarOverrideReader;
import com.tchalanet.server.platform.tenant.internal.port.TenantConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class DefaultTenantBusinessCalendarApi implements TenantBusinessCalendarApi {

    private final TenantConfigReader configReader;
    private final TenantBusinessCalendarOverrideReader overrideReader;

    @Override
    public TenantBusinessDayView resolveBusinessDay(
        TenantId tenantId,
        OutletId outletId,
        LocalDate businessDate
    ) {
        // 1. Outlet-level override (wins over tenant if present)
        if (outletId != null) {
            var outletOverride = overrideReader.findOutletOverride(tenantId, outletId, businessDate);
            if (outletOverride.isPresent()) {
                return outletOverride.get();
            }
        }

        // 2. Tenant-level override
        var tenantOverride = overrideReader.findTenantOverride(tenantId, businessDate);
        return tenantOverride.orElseGet(() -> resolveFromTenantRules(tenantId, businessDate));

        // 3. Recurring rules from tenant config (closedWeekdays + defaultOpen)
    }

    private TenantBusinessDayView resolveFromTenantRules(TenantId tenantId, LocalDate date) {
        var settings = configReader.getInternalSettings(tenantId);
        var calendar = settings != null && settings.rules() != null
            ? settings.rules().businessCalendar()
            : null;

        if (calendar != null && calendar.effectiveClosedWeekdays().contains(date.getDayOfWeek())) {
            return new TenantBusinessDayView(
                tenantId,
                date,
                false,
                "TENANT_CLOSED_WEEKDAY",
                "Tenant is closed on this weekday"
            );
        }

        boolean open = calendar == null || calendar.effectiveDefaultOpen();
        return new TenantBusinessDayView(
            tenantId,
            date,
            open,
            open ? null : "TENANT_DEFAULT_CLOSED",
            open ? null : "Tenant is closed by default"
        );
    }
}
