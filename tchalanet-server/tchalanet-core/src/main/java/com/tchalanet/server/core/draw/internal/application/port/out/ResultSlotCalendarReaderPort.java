package com.tchalanet.server.core.draw.internal.application.port.out;

import com.tchalanet.server.common.types.id.ResultSlotId;

import java.time.LocalDate;
import java.util.Set;

/**
 * Reads the global provider calendar (`result_slot_calendar_override`).
 *
 * <p>Returns the concrete dates (in the slot's own timezone) on which the
 * provider has NO draw for a given result_slot. Both override shapes are
 * resolved:
 * <ul>
 *   <li>specific dated rows ({@code slot_local_date});</li>
 *   <li>year-less recurring rules ({@code recurring_md}, 'MM-dd'), materialized
 *       to each concrete date inside the requested range.</li>
 * </ul>
 *
 * <p>A specific {@code available=true} row overrides a recurring closure for the
 * same day (force-open exception).
 */
public interface ResultSlotCalendarReaderPort {

    /**
     * @return the no-draw dates for {@code resultSlotId} within
     *         {@code [fromInclusive, toInclusive]} (slot timezone).
     */
    Set<LocalDate> findUnavailableDates(
        ResultSlotId resultSlotId,
        LocalDate fromInclusive,
        LocalDate toInclusive);
}
