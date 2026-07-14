package com.tchalanet.server.platform.tenant.api.model.view;

import java.time.DayOfWeek;
import java.time.MonthDay;
import java.util.List;
import java.util.Set;

public record TenantBusinessCalendarRules(
    Boolean defaultOpen, Set<DayOfWeek> closedWeekdays, List<TenantRecurringHolidayRule> holidays) {
  public boolean effectiveDefaultOpen() {
    return defaultOpen == null || defaultOpen;
  }

  public Set<DayOfWeek> effectiveClosedWeekdays() {
    return closedWeekdays == null ? Set.of() : Set.copyOf(closedWeekdays);
  }

  public List<TenantRecurringHolidayRule> effectiveHolidays() {
    return holidays == null ? List.of() : List.copyOf(holidays);
  }

  public record TenantRecurringHolidayRule(
      String key, String monthDay, Boolean open, String label) {
    public boolean effectiveOpen() {
      return Boolean.TRUE.equals(open);
    }

    public MonthDay effectiveMonthDay() {
      return MonthDay.parse("--" + monthDay);
    }
  }
}
