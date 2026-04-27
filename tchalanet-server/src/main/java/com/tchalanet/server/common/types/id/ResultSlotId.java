package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record ResultSlotId(UUID value) {
  public ResultSlotId {
    if (value == null) throw new IllegalArgumentException("ResultSlotId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static ResultSlotId of(UUID value) {
    return new ResultSlotId(value);
  }

  public static ResultSlotId nullableOf(UUID value) {
    return value == null ? null : new ResultSlotId(value);
  }

  public static ResultSlotId parse(String value) {
    return value == null ? null : new ResultSlotId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
