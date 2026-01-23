package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Address. */
public record AddressId(UUID value) {

  public AddressId {
    if (value == null) throw new IllegalArgumentException("AddressId.value is null");
  }

  public static AddressId of(UUID value) {
    return new AddressId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static AddressId nullableOf(UUID raw) {
    return raw == null ? null : new AddressId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static AddressId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("AddressId string is required");
    }
    return new AddressId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
