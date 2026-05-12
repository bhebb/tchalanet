package com.tchalanet.server.common.time;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DaysOfWeekParser {

  public static EnumSet<DayOfWeek> parse(String spec) {
    if (spec == null || spec.isBlank()) {
      throw new IllegalArgumentException("daysOfWeek spec is required");
    }

    String normalized = spec.trim().toUpperCase(Locale.ROOT);

    // Support explicit list separated by comma: MON,TUE,FRI
    if (normalized.contains(",")) {
      EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
      for (String raw : normalized.split(",")) {
        String token = raw.trim();
        if (token.isEmpty()) continue;
        set.add(parseToken(token));
      }
      if (set.isEmpty()) {
        throw new IllegalArgumentException("Invalid daysOfWeek spec: " + spec);
      }
      return set;
    }

    // Support hyphen separated tokens:
    // - Range: MON-SAT
    // - Discrete list: WED-SAT-SUN
    String[] parts = normalized.split("-");
    if (parts.length == 2) {
      DayOfWeek start = parseToken(parts[0].trim());
      DayOfWeek end = parseToken(parts[1].trim());
      return rangeInclusive(start, end);
    }

    EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
    for (String p : parts) {
      String token = p.trim();
      if (token.isEmpty()) continue;
      set.add(parseToken(token));
    }
    if (set.isEmpty()) {
      throw new IllegalArgumentException("Invalid daysOfWeek spec: " + spec);
    }
    return set;
  }

  private static DayOfWeek parseToken(String token) {
    return switch (token) {
      case "1", "MON" -> DayOfWeek.MONDAY;
      case "2", "TUE" -> DayOfWeek.TUESDAY;
      case "3", "WED" -> DayOfWeek.WEDNESDAY;
      case "4", "THU" -> DayOfWeek.THURSDAY;
      case "5", "FRI" -> DayOfWeek.FRIDAY;
      case "6", "SAT" -> DayOfWeek.SATURDAY;
      case "7", "SUN" -> DayOfWeek.SUNDAY;
      default -> throw new IllegalArgumentException("Unknown day token: " + token);
    };
  }

  private static EnumSet<DayOfWeek> rangeInclusive(DayOfWeek start, DayOfWeek end) {
    EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
    DayOfWeek current = start;
    while (true) {
      set.add(current);
      if (current == end) {
        return set;
      }
      current = current.plus(1);
    }
  }
}
