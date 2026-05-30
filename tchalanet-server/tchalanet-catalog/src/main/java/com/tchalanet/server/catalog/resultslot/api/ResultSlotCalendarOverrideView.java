package com.tchalanet.server.catalog.resultslot.api;

import com.tchalanet.server.common.types.id.ResultSlotCalendarOverrideId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.LocalDate;

/**
 * Read-only view of a global provider calendar override (cache-friendly).
 *
 * <p>XOR shape: exactly one of {@code slotLocalDate} (specific dated occurrence)
 * / {@code recurringMd} (year-less 'MM-dd' annual rule) is non-null.
 */
public record ResultSlotCalendarOverrideView(
    ResultSlotCalendarOverrideId id,
    ResultSlotId resultSlotId,
    LocalDate slotLocalDate,
    String recurringMd,
    boolean available,
    String reasonCode,
    String reasonLabel) {}
