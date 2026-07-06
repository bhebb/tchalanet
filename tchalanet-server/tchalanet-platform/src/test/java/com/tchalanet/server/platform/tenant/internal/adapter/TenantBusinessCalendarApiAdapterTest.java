package com.tchalanet.server.platform.tenant.internal.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantBusinessCalendarRules;
import com.tchalanet.server.platform.tenant.api.model.view.TenantBusinessCalendarRules.TenantRecurringHolidayRule;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalRules;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenant.internal.port.TenantBusinessCalendarOverrideReader;
import com.tchalanet.server.platform.tenant.internal.port.TenantConfigReader;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantBusinessCalendarApiAdapterTest {

    private final TenantId tenantId = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000123"));

    @Test
    void oneOffOverrideWinsBeforeRecurringHoliday() {
        var service = service(
            settings(calendar(
                true,
                Set.of(),
                List.of(new TenantRecurringHolidayRule("christmas_day", "12-25", false, "Noel"))
            )),
            (tenant, date) -> Optional.of(new TenantBusinessDayView(tenant, date, true, "MANUAL_OPEN", "Open manually"))
        );

        var day = service.resolveBusinessDay(tenantId, LocalDate.of(2026, 12, 25));

        assertThat(day.open()).isTrue();
        assertThat(day.reasonCode()).isEqualTo("MANUAL_OPEN");
    }

    @Test
    void recurringHolidayIsAppliedBeforeClosedWeekday() {
        var service = service(
            settings(calendar(
                true,
                Set.of(DayOfWeek.FRIDAY),
                List.of(new TenantRecurringHolidayRule("christmas_day", "12-25", true, "Noel ouvert"))
            )),
            (tenant, date) -> Optional.empty()
        );

        var day = service.resolveBusinessDay(tenantId, LocalDate.of(2026, 12, 25));

        assertThat(day.open()).isTrue();
        assertThat(day.reasonCode()).isEqualTo("TENANT_HOLIDAY_OPEN");
        assertThat(day.label()).isEqualTo("Noel ouvert");
    }

    @Test
    void recurringClosedHolidayClosesBusinessDay() {
        var service = service(
            settings(calendar(
                true,
                Set.of(),
                List.of(new TenantRecurringHolidayRule("haiti_independence_day", "01-01", false, "Fete de l'independance"))
            )),
            (tenant, date) -> Optional.empty()
        );

        var day = service.resolveBusinessDay(tenantId, LocalDate.of(2027, 1, 1));

        assertThat(day.open()).isFalse();
        assertThat(day.reasonCode()).isEqualTo("TENANT_HOLIDAY_CLOSED");
        assertThat(day.label()).isEqualTo("Fete de l'independance");
    }

    private TenantBusinessCalendarApiAdapter service(
        TenantInternalSettings settings,
        TenantBusinessCalendarOverrideReader overrideReader
    ) {
        TenantConfigReader configReader = tenant -> settings;
        return new TenantBusinessCalendarApiAdapter(configReader, overrideReader);
    }

    private TenantInternalSettings settings(TenantBusinessCalendarRules calendar) {
        return new TenantInternalSettings(null, null, new TenantInternalRules(calendar), null);
    }

    private TenantBusinessCalendarRules calendar(
        Boolean defaultOpen,
        Set<DayOfWeek> closedWeekdays,
        List<TenantRecurringHolidayRule> holidays
    ) {
        return new TenantBusinessCalendarRules(defaultOpen, closedWeekdays, holidays);
    }
}
