package com.tchalanet.server.core.draw.internal.infra.persistence.adapter;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarOverrideView;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.draw.internal.application.port.out.ResultSlotCalendarReaderPort;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reads provider no-draw dates via the cached {@link ResultSlotCalendarCatalog}
 * (24h TTL). The catalog owns storage + cache; this adapter owns the date logic:
 * it materializes recurring 'MM-dd' rules across the requested range and lets a
 * specific {@code available=true} row override a recurring closure.
 */
@Component
@RequiredArgsConstructor
public class ResultSlotCalendarReaderAdapter implements ResultSlotCalendarReaderPort {

  private static final DateTimeFormatter MM_DD = DateTimeFormatter.ofPattern("MM-dd");

  private final ResultSlotCalendarCatalog calendarCatalog;

  @Override
  public Set<LocalDate> findUnavailableDates(
      ResultSlotId resultSlotId, LocalDate fromInclusive, LocalDate toInclusive) {

    Objects.requireNonNull(resultSlotId, "resultSlotId is required");
    Objects.requireNonNull(fromInclusive, "fromInclusive is required");
    Objects.requireNonNull(toInclusive, "toInclusive is required");
    if (toInclusive.isBefore(fromInclusive)) {
      return Set.of();
    }

    var overrides = calendarCatalog.listBySlot(resultSlotId);
    if (overrides.isEmpty()) {
      return Set.of();
    }

    Set<LocalDate> unavailable = new HashSet<>();
    Set<LocalDate> forceOpen = new HashSet<>();

    for (var ov : overrides) {
      if (ov.slotLocalDate() != null) {
        if (inRange(ov.slotLocalDate(), fromInclusive, toInclusive)) {
          (ov.available() ? forceOpen : unavailable).add(ov.slotLocalDate());
        }
      } else if (ov.recurringMd() != null && !ov.available()) {
        unavailable.addAll(materialize(ov.recurringMd(), fromInclusive, toInclusive));
      }
    }

    // A specific available=true row overrides any recurring closure for that day.
    unavailable.removeAll(forceOpen);
    return unavailable;
  }

  private static boolean inRange(LocalDate d, LocalDate from, LocalDate to) {
    return !d.isBefore(from) && !d.isAfter(to);
  }

  private static Set<LocalDate> materialize(String md, LocalDate from, LocalDate to) {
    MonthDay monthDay;
    try {
      monthDay = MonthDay.parse(md, MM_DD);
    } catch (RuntimeException e) {
      return Set.of();
    }
    Set<LocalDate> dates = new HashSet<>();
    for (int year = from.getYear(); year <= to.getYear(); year++) {
      if (!monthDay.isValidYear(year)) {
        continue; // e.g. Feb 29 in a non-leap year
      }
      var date = monthDay.atYear(year);
      if (inRange(date, from, to)) {
        dates.add(date);
      }
    }
    return dates;
  }
}
