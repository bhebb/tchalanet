package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record SessionId(UUID value) {
  public SessionId {
    if (value == null) throw new IllegalArgumentException("SessionId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static SessionId of(UUID value) {
    return new SessionId(value);
  }

  public static SessionId nullableOf(UUID value) {
    return value == null ? null : new SessionId(value);
  }

  public static SessionId parse(String value) {
    return value == null ? null : new SessionId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
