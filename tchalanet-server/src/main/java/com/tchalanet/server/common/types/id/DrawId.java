package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record DrawId(UUID value) {
  public DrawId {
    if (value == null) throw new IllegalArgumentException("DrawId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static DrawId of(UUID value) {
    return new DrawId(value);
  }

  public static DrawId nullableOf(UUID value) {
    return value == null ? null : new DrawId(value);
  }

  public static DrawId parse(String value) {
    return value == null ? null : new DrawId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
