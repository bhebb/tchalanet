package com.tchalanet.server.platform.archive.api.model;

import java.time.LocalDate;

/**
 * Half-open date interval {@code [start, end)} for an archive run.
 *
 * <p>For monthly archival, {@code start} is the first day of the month and
 * {@code end} is the first day of the following month.
 */
public record ArchivePeriod(LocalDate start, LocalDate end) {

  public static ArchivePeriod of(LocalDate start, LocalDate end) {
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("ArchivePeriod start must be before end");
    }
    return new ArchivePeriod(start, end);
  }

  public static ArchivePeriod forMonth(int year, int month) {
    LocalDate start = LocalDate.of(year, month, 1);
    return new ArchivePeriod(start, start.plusMonths(1));
  }
}
