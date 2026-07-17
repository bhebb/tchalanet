package com.tchalanet.server.core.drawresult.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarOverrideView;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.ResultSlotCalendarOverrideId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResultReminderExpirationPolicyTest {

  @Test
  void expiresOneMinuteBeforeNextOccurrenceWhenSoonerThanTwentyFourHours() {
    var slot = slot(LocalTime.of(20, 0), "MON-SUN");
    var policy = new ResultReminderExpirationPolicy(emptyCalendar());
    var createdAt = Instant.parse("2026-07-16T18:00:00Z");

    assertThat(policy.expiresAt(slot, createdAt))
        .isEqualTo(Instant.parse("2026-07-16T23:59:00Z"));
  }

  @Test
  void expiresAtTwentyFourHoursWhenNextOccurrenceIsLater() {
    var slot = slot(LocalTime.of(20, 0), "MON");
    var policy = new ResultReminderExpirationPolicy(emptyCalendar());
    var createdAt = Instant.parse("2026-07-14T18:00:00Z");

    assertThat(policy.expiresAt(slot, createdAt))
        .isEqualTo(Instant.parse("2026-07-15T18:00:00Z"));
  }

  @Test
  void skipsUnavailableSpecificOverrideWhenComputingNextOccurrence() {
    var slot = slot(LocalTime.of(20, 0), "MON-SUN");
    var override =
        new ResultSlotCalendarOverrideView(
            ResultSlotCalendarOverrideId.of(UUID.randomUUID()),
            slot.id(),
            LocalDate.of(2026, 7, 16),
            null,
            false,
            "CLOSED",
            "Closed");
    var policy = new ResultReminderExpirationPolicy(resultSlotId -> List.of(override));
    var createdAt = Instant.parse("2026-07-16T18:00:00Z");

    assertThat(policy.expiresAt(slot, createdAt))
        .isEqualTo(Instant.parse("2026-07-17T18:00:00Z"));
  }

  private static ResultSlotCalendarCatalog emptyCalendar() {
    return resultSlotId -> List.of();
  }

  private static ResultSlotView slot(LocalTime drawTime, String daysOfWeek) {
    return new ResultSlotView(
        ResultSlotId.of(UUID.randomUUID()),
        "FL_EVE",
        "FL",
        ZoneId.of("America/New_York"),
        drawTime,
        daysOfWeek,
        true,
        JsonUtils.emptyObject(),
        JsonUtils.emptyObject(),
        "slot.fl.eve");
  }
}
