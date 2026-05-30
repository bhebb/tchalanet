package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record BusinessDayOverrideId(UUID value) {
  public BusinessDayOverrideId {
    if (value == null) throw new IllegalArgumentException("BusinessDayOverrideId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static BusinessDayOverrideId of(UUID value) {
    return new BusinessDayOverrideId(value);
  }

  public static BusinessDayOverrideId nullableOf(UUID value) {
    return value == null ? null : new BusinessDayOverrideId(value);
  }

  public static BusinessDayOverrideId parse(String value) {
    return value == null ? null : new BusinessDayOverrideId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
