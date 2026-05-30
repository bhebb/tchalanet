package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record ResultSlotCalendarOverrideId(UUID value) {
  public ResultSlotCalendarOverrideId {
    if (value == null) throw new IllegalArgumentException("ResultSlotCalendarOverrideId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static ResultSlotCalendarOverrideId of(UUID value) {
    return new ResultSlotCalendarOverrideId(value);
  }

  public static ResultSlotCalendarOverrideId nullableOf(UUID value) {
    return value == null ? null : new ResultSlotCalendarOverrideId(value);
  }

  public static ResultSlotCalendarOverrideId parse(String value) {
    return value == null ? null : new ResultSlotCalendarOverrideId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
