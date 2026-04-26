package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OutletId(UUID value) {
  public OutletId {
    if (value == null) throw new IllegalArgumentException("OutletId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static OutletId of(UUID value) {
    return new OutletId(value);
  }

  public static OutletId nullableOf(UUID value) {
    return value == null ? null : new OutletId(value);
  }

  public static OutletId parse(String value) {
    return value == null ? null : new OutletId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
