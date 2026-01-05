package com.tchalanet.server.common.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class TimeProvider {

  private final Clock clock; // clock global (UTC ou app.zone-id)

  public TimeProvider(Clock clock) {
    this.clock = clock;
  }

  public ZonedDateTime now(ZoneId zone) {
    // utilise l'instant du clock (testable) + applique une zone différente
    return ZonedDateTime.now(clock).withZoneSameInstant(zone);
  }

  public LocalDate today(ZoneId zone) {
    return now(zone).toLocalDate();
  }
}

