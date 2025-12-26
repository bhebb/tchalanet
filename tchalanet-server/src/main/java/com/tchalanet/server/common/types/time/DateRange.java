package com.tchalanet.server.common.types.time;

import java.time.LocalDate;
import java.util.Objects;

/** Value object for a date range with inclusive start and end. */
public record DateRange(LocalDate start, LocalDate end) {

  public DateRange {
    Objects.requireNonNull(start, "DateRange.start is null");
    Objects.requireNonNull(end, "DateRange.end is null");
    if (start.isAfter(end)) throw new IllegalArgumentException("DateRange.start is after end");
  }

  public static DateRange of(LocalDate start, LocalDate end) {
    return new DateRange(start, end);
  }
}
