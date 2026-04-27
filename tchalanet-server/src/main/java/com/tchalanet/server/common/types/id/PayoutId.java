package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record PayoutId(UUID value) {
  public PayoutId {
    if (value == null) throw new IllegalArgumentException("PayoutId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static PayoutId of(UUID value) {
    return new PayoutId(value);
  }

  public static PayoutId nullableOf(UUID value) {
    return value == null ? null : new PayoutId(value);
  }

  public static PayoutId parse(String value) {
    return value == null ? null : new PayoutId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
