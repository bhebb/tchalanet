package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record DrawResultId(UUID value) {
  public DrawResultId {
    if (value == null) throw new IllegalArgumentException("DrawResultId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static DrawResultId of(UUID value) {
    return new DrawResultId(value);
  }

  public static DrawResultId nullableOf(UUID value) {
    return value == null ? null : new DrawResultId(value);
  }

  public static DrawResultId parse(String value) {
    return value == null ? null : new DrawResultId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
