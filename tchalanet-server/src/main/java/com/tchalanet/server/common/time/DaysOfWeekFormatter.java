package com.tchalanet.server.common.time;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class DaysOfWeekFormatter {

  private DaysOfWeekFormatter() {}

  public static String format(List<DayOfWeek> days) {
    if (days == null || days.isEmpty()) return "";
    if (days.size() == 7) return "MON-SUN";
    List<DayOfWeek> sorted = days.stream().distinct().sorted(Comparator.comparingInt(DayOfWeek::getValue)).collect(Collectors.toList());
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
      if (expected > 7) expected = 1;
    }
    if (contiguous && sorted.size() > 1) {
      return start.name().substring(0,3) + "-" + end.name().substring(0,3);
    }
    return sorted.stream().map(d -> d.name().substring(0,3)).collect(Collectors.joining(","));
  }

  public static List<DayOfWeek> parse(String days) {
    if (days == null || days.isBlank()) return List.of();

    // Handle MON-SUN range
    if ("MON-SUN".equalsIgnoreCase(days.trim())) {
      return Arrays.asList(DayOfWeek.values());
    }

    // Handle comma-separated three-letter tokens or ranges like MON-FRI
    return Arrays.stream(days.split(","))
        .map(String::trim)
        .flatMap(part -> {
            if (part.contains("-")) {
                String[] range = part.split("-");
                DayOfWeek start = parseToken(range[0]);
                DayOfWeek end = parseToken(range[1]);
                return java.util.stream.Stream.iterate(start, d -> d.plus(1))
                    .limit((end.getValue() - start.getValue() + 7) % 7 + 1);
            } else {
                return java.util.stream.Stream.of(parseToken(part));
            }
        })
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  private static DayOfWeek parseToken(String token) {
    String t = token.trim().toUpperCase();
    return switch (t) {
        case "MON" -> DayOfWeek.MONDAY;
        case "TUE" -> DayOfWeek.TUESDAY;
        case "WED" -> DayOfWeek.WEDNESDAY;
        case "THU" -> DayOfWeek.THURSDAY;
        case "FRI" -> DayOfWeek.FRIDAY;
        case "SAT" -> DayOfWeek.SATURDAY;
        case "SUN" -> DayOfWeek.SUNDAY;
        default -> throw new IllegalArgumentException("Unknown day token: " + t);
    };
  }
}
