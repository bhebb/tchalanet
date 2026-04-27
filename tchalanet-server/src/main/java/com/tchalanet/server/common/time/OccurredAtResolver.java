package com.tchalanet.server.common.time;

import java.time.*;

public final class OccurredAtResolver {
  private OccurredAtResolver() {}

  public static Instant resolve(
      Instant primary, LocalDate drawDate, LocalTime drawTime, ZoneId zone, Clock clock) {

    if (primary != null) return primary;
    if (drawDate != null && drawTime != null && zone != null) {
      return ZonedDateTime.of(drawDate, drawTime, zone).toInstant();
    }
    return Instant.now(clock);
  }
}
