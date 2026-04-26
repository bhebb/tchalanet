package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record RoleId(UUID value) {
  public RoleId {
    if (value == null) throw new IllegalArgumentException("RoleId is null");
  }

  public UUID uuid() {
    return value;
  }

  public static RoleId of(UUID value) {
    return new RoleId(value);
  }

  public static RoleId nullableOf(UUID value) {
    return value == null ? null : new RoleId(value);
  }

  public static RoleId parse(String value) {
    return value == null ? null : new RoleId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
