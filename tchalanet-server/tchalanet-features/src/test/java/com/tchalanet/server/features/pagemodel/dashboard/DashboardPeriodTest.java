package com.tchalanet.server.features.pagemodel.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Test;

class DashboardPeriodTest {

  @Test
  void parseNullDefaultsToToday() {
    assertThat(DashboardPeriod.parse(null)).isEqualTo(DashboardPeriod.TODAY);
  }

  @Test
  void parseBlankDefaultsToToday() {
    assertThat(DashboardPeriod.parse("  ")).isEqualTo(DashboardPeriod.TODAY);
  }

  @Test
  void parseUnknownValueDefaultsToToday() {
    assertThat(DashboardPeriod.parse("NOPE")).isEqualTo(DashboardPeriod.TODAY);
  }

  @Test
  void parseValidValueCaseInsensitive() {
    assertThat(DashboardPeriod.parse("yesterday")).isEqualTo(DashboardPeriod.YESTERDAY);
    assertThat(DashboardPeriod.parse("THIS_WEEK")).isEqualTo(DashboardPeriod.THIS_WEEK);
    assertThat(DashboardPeriod.parse("last_week")).isEqualTo(DashboardPeriod.LAST_WEEK);
  }

  @Test
  void todayWindowsAreTodayAndYesterday() {
    var today = LocalDate.of(2026, 7, 22);
    var windows = DashboardPeriod.TODAY.windows(today);

    assertThat(windows.from()).isEqualTo(today);
    assertThat(windows.to()).isEqualTo(today);
    assertThat(windows.comparisonFrom()).isEqualTo(today.minusDays(1));
    assertThat(windows.comparisonTo()).isEqualTo(today.minusDays(1));
  }

  @Test
  void yesterdayWindowsAreCorrect() {
    var today = LocalDate.of(2026, 7, 22);
    var windows = DashboardPeriod.YESTERDAY.windows(today);

    assertThat(windows.from()).isEqualTo(LocalDate.of(2026, 7, 21));
    assertThat(windows.to()).isEqualTo(LocalDate.of(2026, 7, 21));
    assertThat(windows.comparisonFrom()).isEqualTo(LocalDate.of(2026, 7, 20));
  }

  @Test
  void thisWeekWindowsStartOnMonday() {
    var today = LocalDate.of(2026, 7, 22); // Wednesday
    var windows = DashboardPeriod.THIS_WEEK.windows(today);

    var expectedWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    assertThat(windows.from()).isEqualTo(expectedWeekStart);
    assertThat(windows.to()).isEqualTo(today);
    assertThat(windows.comparisonFrom()).isEqualTo(expectedWeekStart.minusDays(7));
    assertThat(windows.comparisonTo()).isEqualTo(expectedWeekStart.minusDays(1));
  }

  @Test
  void lastWeekWindowsCoverPreviousWeek() {
    var today = LocalDate.of(2026, 7, 22); // Wednesday
    var windows = DashboardPeriod.LAST_WEEK.windows(today);

    var currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    assertThat(windows.from()).isEqualTo(currentWeekStart.minusDays(7));
    assertThat(windows.to()).isEqualTo(currentWeekStart.minusDays(1));
    assertThat(windows.comparisonFrom()).isEqualTo(currentWeekStart.minusDays(14));
    assertThat(windows.comparisonTo()).isEqualTo(currentWeekStart.minusDays(8));
  }
}
