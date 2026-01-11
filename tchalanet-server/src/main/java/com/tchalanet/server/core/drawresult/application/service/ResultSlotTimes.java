package com.tchalanet.server.core.drawresult.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class ResultSlotTimes {
  private ResultSlotTimes() {}

  public static Instant occurredAt(ZoneId tz, LocalDate drawDate, LocalTime drawTime) {
    // drawDate est une date “locale slot”. drawTime est l’heure du tirage.
    // On crée un ZonedDateTime dans la timezone du slot puis on convertit en Instant.
    return ZonedDateTime.of(drawDate, drawTime, tz).toInstant();
  }
}
