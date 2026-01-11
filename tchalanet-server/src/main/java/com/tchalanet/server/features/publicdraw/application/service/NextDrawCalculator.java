package com.tchalanet.server.features.publicdraw.application.service;

import com.tchalanet.server.common.time.DaysOfWeekParser;
import com.tchalanet.server.common.time.TimeProvider;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NextDrawCalculator {

  private final TimeProvider time; // testable clock

  public java.time.Instant nextScheduledAt(
      String timezone, LocalTime drawTime, String daysOfWeekCsv) {
    if (timezone == null || drawTime == null) return null;

    var zone = java.time.ZoneId.of(timezone);
    var now = time.now(zone);
    var candidate = java.time.ZonedDateTime.of(now.toLocalDate(), drawTime, zone);

    if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);

    // daysOfWeek optional: "1,2,3,4,5,6,7" (1=Mon)
    if (daysOfWeekCsv != null && !daysOfWeekCsv.isBlank()) {
      var allowed = DaysOfWeekParser.parse(daysOfWeekCsv); // EnumSet<DayOfWeek>

      for (int i = 0; i < 14; i++) {
        if (allowed.contains(candidate.getDayOfWeek().getValue())) break;
        candidate = candidate.plusDays(1);
      }
    }

    return candidate.toInstant();
  }
}
