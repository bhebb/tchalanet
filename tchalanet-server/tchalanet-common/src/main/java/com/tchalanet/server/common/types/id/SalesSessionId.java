package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record SalesSessionId(UUID value) {
  public SalesSessionId {
    if (value == null) throw new IllegalArgumentException("SessionId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static SalesSessionId of(UUID value) {
    return new SalesSessionId(value);
  }

  public static SalesSessionId nullableOf(UUID value) {
    return value == null ? null : new SalesSessionId(value);
  }

  public static SalesSessionId parse(String value) {
    return value == null ? null : new SalesSessionId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
