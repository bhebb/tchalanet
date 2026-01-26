package com.tchalanet.server.common.time;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class DaysOfWeekFormatter {

  private DaysOfWeekFormatter() {}

  public static String format(List<DayOfWeek> days) {
    if (days == null || days.isEmpty()) return "";
    // If contains all days, return MON-SUN
    if (days.size() == 7) return "MON-SUN";
    // Normalize to sorted unique list
    List<DayOfWeek> sorted = days.stream().distinct().sorted(Comparator.comparingInt(DayOfWeek::getValue)).collect(Collectors.toList());
    // If contiguous range
    DayOfWeek start = sorted.get(0);
    DayOfWeek end = sorted.get(sorted.size() - 1);
    boolean contiguous = true;
    int expected = start.getValue();
    for (DayOfWeek d : sorted) {
      if (d.getValue() != expected) {
        contiguous = false;
        break;
      }
      expected++;
      if (expected > 7) expected = 1; // wrap
    }
    if (contiguous) {
      return start.name().substring(0,3) + "-" + end.name().substring(0,3);
    }
    // else comma-separated three-letter tokens
    return sorted.stream().map(d -> d.name().substring(0,3)).collect(Collectors.joining(","));
  }
}
