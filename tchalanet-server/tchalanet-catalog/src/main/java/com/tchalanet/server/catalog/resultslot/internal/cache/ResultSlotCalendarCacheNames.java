package com.tchalanet.server.catalog.resultslot.internal.cache;

/** Cache names for the global provider calendar (result_slot_calendar_override). */
public final class ResultSlotCalendarCacheNames {
  private ResultSlotCalendarCacheNames() {}

  /** Live overrides for a single result_slot, keyed by slot id. */
  public static final String BY_SLOT = "catalog:resultslot:calendar:v1:by_slot";
}
