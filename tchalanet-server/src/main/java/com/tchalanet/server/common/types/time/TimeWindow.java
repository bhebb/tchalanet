package com.tchalanet.server.common.types.time;

import java.time.LocalDateTime;

/** Value object for time windows. */
public record TimeWindow(LocalDateTime start, LocalDateTime end) {

  public TimeWindow {
    if (start == null) throw new IllegalArgumentException("TimeWindow.start is null");
    if (end == null) throw new IllegalArgumentException("TimeWindow.end is null");
    if (start.isAfter(end)) throw new IllegalArgumentException("TimeWindow.start is after end");
  }

  /**
   * Static factory for TimeWindow.
   */
  public static TimeWindow of(LocalDateTime start, LocalDateTime end) {
    return new TimeWindow(start, end);
  }
}
