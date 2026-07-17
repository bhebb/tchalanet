package com.tchalanet.server.core.drawresult.internal.application.service;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarOverrideView;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.stereotype.UseCase;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ResultReminderExpirationPolicy {

  private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");

  private final ResultSlotCalendarCatalog calendarCatalog;

  public Instant expiresAt(ResultSlotView slot, Instant createdAt) {
    var ttl = createdAt.plus(Duration.ofHours(24));
    var nextOccurrence = nextOccurrence(slot, createdAt);
    if (nextOccurrence == null || !nextOccurrence.isAfter(createdAt)) {
      return ttl;
    }
    var beforeNext = nextOccurrence.minus(Duration.ofMinutes(1));
    if (!beforeNext.isAfter(createdAt)) {
      return ttl;
    }
    return beforeNext.isBefore(ttl) ? beforeNext : ttl;
  }

  private Instant nextOccurrence(ResultSlotView slot, Instant createdAt) {
    if (slot == null || slot.timezone() == null || slot.drawTime() == null) {
      return null;
    }

    var allowedDays = ResultSlotDaysOfWeekPolicy.parse(slot.daysOfWeek());
    var localStart = createdAt.atZone(slot.timezone()).toLocalDate();
    var overrides = calendarCatalog.listBySlot(slot.id());

    for (int offset = 0; offset <= 14; offset++) {
      var date = localStart.plusDays(offset);
      if (!allowedDays.contains(date.getDayOfWeek())) {
        continue;
      }
      if (!isAvailable(date, overrides)) {
        continue;
      }
      var occurrence = date.atTime(slot.drawTime()).atZone(slot.timezone()).toInstant();
      if (occurrence.isAfter(createdAt)) {
        return occurrence;
      }
    }
    return null;
  }

  private boolean isAvailable(LocalDate date, Iterable<ResultSlotCalendarOverrideView> overrides) {
    for (var override : overrides) {
      if (override.slotLocalDate() != null && override.slotLocalDate().equals(date)) {
        return override.available();
      }
      if (override.recurringMd() != null && override.recurringMd().equals(MONTH_DAY.format(date))) {
        return override.available();
      }
    }
    return true;
  }
}
