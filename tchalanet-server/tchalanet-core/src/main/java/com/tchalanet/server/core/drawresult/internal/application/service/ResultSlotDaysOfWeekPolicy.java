package com.tchalanet.server.core.drawresult.internal.application.service;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

final class ResultSlotDaysOfWeekPolicy {

  private ResultSlotDaysOfWeekPolicy() {}

  static Set<DayOfWeek> parse(String raw) {
    if (raw == null || raw.isBlank()) {
      return EnumSet.allOf(DayOfWeek.class);
    }

    var normalized = raw.trim().toUpperCase(Locale.ROOT);
    if ("MON-SUN".equals(normalized) || "ALL".equals(normalized) || "*".equals(normalized)) {
      return EnumSet.allOf(DayOfWeek.class);
    }
    if ("MON-SAT".equals(normalized)) {
      return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);
    }
    if ("MON-FRI".equals(normalized)) {
      return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    }

    var out = EnumSet.noneOf(DayOfWeek.class);
    for (var token : normalized.split(",")) {
      var day = parseOne(token.trim());
      if (day != null) {
        out.add(day);
      }
    }
    return out.isEmpty() ? EnumSet.allOf(DayOfWeek.class) : out;
  }

  private static DayOfWeek parseOne(String token) {
    return switch (token) {
      case "MON", "MONDAY" -> DayOfWeek.MONDAY;
      case "TUE", "TUESDAY" -> DayOfWeek.TUESDAY;
      case "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
      case "THU", "THURSDAY" -> DayOfWeek.THURSDAY;
      case "FRI", "FRIDAY" -> DayOfWeek.FRIDAY;
      case "SAT", "SATURDAY" -> DayOfWeek.SATURDAY;
      case "SUN", "SUNDAY" -> DayOfWeek.SUNDAY;
      default -> null;
    };
  }
}
