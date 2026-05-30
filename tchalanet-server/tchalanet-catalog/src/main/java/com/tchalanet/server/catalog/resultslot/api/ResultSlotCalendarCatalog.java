package com.tchalanet.server.catalog.resultslot.api;

import com.tchalanet.server.common.types.id.ResultSlotId;
import java.util.List;

/**
 * Public contract for the global provider calendar
 * ({@code result_slot_calendar_override}), read-only and cacheable (24h).
 *
 * <p>Returns the raw live overrides for a slot. Date materialization
 * (recurring 'MM-dd' → concrete dates, specific-overrides-recurring) is the
 * caller's responsibility — see {@code core.draw} reader.
 */
public interface ResultSlotCalendarCatalog {

  /** All live overrides for a result_slot (both specific and recurring shapes). */
  List<ResultSlotCalendarOverrideView> listBySlot(ResultSlotId resultSlotId);
}
