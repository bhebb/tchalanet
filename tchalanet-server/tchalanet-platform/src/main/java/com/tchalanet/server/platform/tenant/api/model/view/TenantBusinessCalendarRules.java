package com.tchalanet.server.platform.tenant.api.model.view;

import java.time.DayOfWeek;
import java.util.Set;

public record TenantBusinessCalendarRules(
    Boolean defaultOpen,
    Set<DayOfWeek> closedWeekdays,
    Boolean holidaySalesAllowed
) {
    public boolean effectiveDefaultOpen() {
        return defaultOpen == null || defaultOpen;
    }

    public Set<DayOfWeek> effectiveClosedWeekdays() {
        return closedWeekdays == null ? Set.of() : Set.copyOf(closedWeekdays);
    }

    public boolean effectiveHolidaySalesAllowed() {
        return Boolean.TRUE.equals(holidaySalesAllowed);
    }
}
