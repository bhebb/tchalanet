package com.tchalanet.server.common.time;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

public final class DateWindows {

  private DateWindows() {}

  /**
   * Returns an inclusive list of dates from baseDate back to baseDate - daysBack. Example for
   * baseDate=2026-01-07 and daysBack=2 -> [2026-01-07, 2026-01-06, 2026-01-05]
   */
  public static List<LocalDate> datesBackInclusive(LocalDate baseDate, int daysBack) {
    if (baseDate == null) throw new IllegalArgumentException("baseDate required");
    if (daysBack < 0) throw new IllegalArgumentException("daysBack must be >= 0");

    return IntStream.rangeClosed(0, daysBack).mapToObj(baseDate::minusDays).toList();
  }
}
