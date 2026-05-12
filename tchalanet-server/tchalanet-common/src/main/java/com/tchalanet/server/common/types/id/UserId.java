package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record UserId(UUID value) {
  public UserId {
    if (value == null) throw new IllegalArgumentException("UserId is null");
  }

  public UUID uuid() {
    return value;
  }

  public static UserId of(UUID value) {
    return new UserId(value);
  }

  public static UserId nullableOf(UUID value) {
    return value == null ? null : new UserId(value);
  }

  public static UserId parse(String value) {
    return value == null ? null : new UserId(UUID.fromString(value));
  }
}
