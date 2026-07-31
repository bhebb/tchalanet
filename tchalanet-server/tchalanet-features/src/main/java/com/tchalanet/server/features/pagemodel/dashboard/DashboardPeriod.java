package com.tchalanet.server.features.pagemodel.dashboard;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** Tenant-local dashboard windows and their comparison windows. */
public enum DashboardPeriod {
  TODAY,
  YESTERDAY,
  THIS_WEEK,
  LAST_WEEK;

  public static DashboardPeriod parse(String value) {
    if (value == null || value.isBlank()) {
      return TODAY;
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return TODAY;
    }
  }

  public Windows windows(LocalDate today) {
    LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return switch (this) {
      case TODAY -> new Windows(today, today, today.minusDays(1), today.minusDays(1));
      case YESTERDAY ->
          new Windows(
              today.minusDays(1), today.minusDays(1), today.minusDays(2), today.minusDays(2));
      case THIS_WEEK ->
          new Windows(
              currentWeekStart,
              today,
              currentWeekStart.minusDays(7),
              currentWeekStart.minusDays(1));
      case LAST_WEEK ->
          new Windows(
              currentWeekStart.minusDays(7),
              currentWeekStart.minusDays(1),
              currentWeekStart.minusDays(14),
              currentWeekStart.minusDays(8));
    };
  }

  public record Windows(
      LocalDate from, LocalDate to, LocalDate comparisonFrom, LocalDate comparisonTo) {}
}
