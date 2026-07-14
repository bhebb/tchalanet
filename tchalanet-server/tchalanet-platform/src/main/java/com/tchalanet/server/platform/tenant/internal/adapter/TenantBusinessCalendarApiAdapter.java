package com.tchalanet.server.platform.tenant.internal.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.TenantBusinessCalendarApi;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;
import com.tchalanet.server.platform.tenant.internal.port.TenantBusinessCalendarOverrideReader;
import com.tchalanet.server.platform.tenant.internal.port.TenantConfigReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantBusinessCalendarApiAdapter implements TenantBusinessCalendarApi {

  private static final DateTimeFormatter MONTH_DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

  private final TenantConfigReader configReader;
  private final TenantBusinessCalendarOverrideReader overrideReader;

  @Override
  public TenantBusinessDayView resolveBusinessDay(TenantId tenantId, LocalDate businessDate) {
    var tenantOverride = overrideReader.findTenantOverride(tenantId, businessDate);
    return tenantOverride.orElseGet(() -> resolveFromTenantRules(tenantId, businessDate));
  }

  private TenantBusinessDayView resolveFromTenantRules(TenantId tenantId, LocalDate date) {
    var settings = configReader.getInternalSettings(tenantId);
    var calendar =
        settings != null && settings.rules() != null ? settings.rules().businessCalendar() : null;

    var monthDay = date.format(MONTH_DAY_FORMAT);
    var holiday =
        calendar == null
            ? null
            : calendar.effectiveHolidays().stream()
                .filter(rule -> monthDay.equals(rule.monthDay()))
                .findFirst()
                .orElse(null);
    if (holiday != null) {
      var open = holiday.effectiveOpen();
      return new TenantBusinessDayView(
          tenantId,
          date,
          open,
          open ? "TENANT_HOLIDAY_OPEN" : "TENANT_HOLIDAY_CLOSED",
          holiday.label() == null || holiday.label().isBlank()
              ? defaultHolidayLabel(holiday.key(), open)
              : holiday.label());
    }

    if (calendar != null && calendar.effectiveClosedWeekdays().contains(date.getDayOfWeek())) {
      return new TenantBusinessDayView(
          tenantId, date, false, "TENANT_CLOSED_WEEKDAY", "Tenant is closed on this weekday");
    }

    boolean open = calendar == null || calendar.effectiveDefaultOpen();
    return new TenantBusinessDayView(
        tenantId,
        date,
        open,
        open ? null : "TENANT_DEFAULT_CLOSED",
        open ? null : "Tenant is closed by default");
  }

  private String defaultHolidayLabel(String key, boolean open) {
    var normalized =
        key == null || key.isBlank()
            ? "holiday"
            : key.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
    return open ? "Tenant is open on " + normalized : "Tenant is closed for " + normalized;
  }
}
