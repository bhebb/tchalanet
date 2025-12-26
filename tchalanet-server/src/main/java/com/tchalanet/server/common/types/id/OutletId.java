package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Value object identifier for Outlet. */
public record OutletId(UUID value) {

  public OutletId {
    if (value == null) throw new IllegalArgumentException("OutletId.value is null");
  }

  /**
   * Static factory from UUID.
   */
  public static OutletId of(UUID value) {
    return new OutletId(value);
  }

  /** Return OutletId or null if id is null */
  public static OutletId nullableOf(UUID id) { return id == null ? null : new OutletId(id); }

  /**
   * Static factory from String representation of UUID.
   * @throws IllegalArgumentException if the string is not a valid UUID
   */
  public static OutletId of(String id) {
    if (id == null) throw new IllegalArgumentException("outlet id string is required");
    return new OutletId(UUID.fromString(id));
  }

  public static OutletId random() {
    return new OutletId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() { return value; }
}
