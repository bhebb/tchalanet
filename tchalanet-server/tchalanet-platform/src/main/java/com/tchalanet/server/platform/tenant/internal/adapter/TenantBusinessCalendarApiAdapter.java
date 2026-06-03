package com.tchalanet.server.platform.tenant.internal.adapter;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.TenantBusinessCalendarApi;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;
import com.tchalanet.server.platform.tenant.internal.port.TenantBusinessCalendarOverrideReader;
import com.tchalanet.server.platform.tenant.internal.port.TenantConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TenantBusinessCalendarApiAdapter implements TenantBusinessCalendarApi {

    private final TenantConfigReader configReader;
    private final TenantBusinessCalendarOverrideReader overrideReader;

    @Override
    public TenantBusinessDayView resolveBusinessDay(
        TenantId tenantId,
        OutletId outletId,
        LocalDate businessDate
    ) {
        if (outletId != null) {
            var outletOverride = overrideReader.findOutletOverride(tenantId, outletId, businessDate);
            if (outletOverride.isPresent()) {
                return outletOverride.get();
            }
        }
        var tenantOverride = overrideReader.findTenantOverride(tenantId, businessDate);
        return tenantOverride.orElseGet(() -> resolveFromTenantRules(tenantId, businessDate));
    }

    private TenantBusinessDayView resolveFromTenantRules(TenantId tenantId, LocalDate date) {
        var settings = configReader.getInternalSettings(tenantId);
        var calendar = settings != null && settings.rules() != null
            ? settings.rules().businessCalendar()
            : null;

        if (calendar != null && calendar.effectiveClosedWeekdays().contains(date.getDayOfWeek())) {
            return new TenantBusinessDayView(tenantId, date, false,
                "TENANT_CLOSED_WEEKDAY", "Tenant is closed on this weekday");
        }

        boolean open = calendar == null || calendar.effectiveDefaultOpen();
        return new TenantBusinessDayView(tenantId, date, open,
            open ? null : "TENANT_DEFAULT_CLOSED",
            open ? null : "Tenant is closed by default");
    }
}
