package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Value object identifier for User. */
public record UserId(UUID value) {

  public UserId {
    if (value == null) throw new IllegalArgumentException("UserId.value is null");
  }

  /** Static factory from UUID. */
  public static UserId of(UUID value) {
    return new UserId(value);
  }

  /**
   * Return a UserId for the given UUID or null if the uuid is null. Useful for optional mappings.
   */
  public static UserId nullableOf(UUID id) {
    return id == null ? null : new UserId(id);
  }

  /**
   * Static factory from String representation of UUID.
   *
   * @throws IllegalArgumentException if the string is not a valid UUID
   */
  public static UserId of(String id) {
    if (id == null) throw new IllegalArgumentException("user id string is required");
    return new UserId(UUID.fromString(id));
  }

  public static UserId random() {
    return new UserId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() {
    return value;
  }
}
