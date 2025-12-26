package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Value object identifier for Draw. */
public record DrawId(UUID value) {

  public DrawId {
    if (value == null) throw new IllegalArgumentException("DrawId.value is null");
  }

  /** Static factory from UUID. */
  public static DrawId of(UUID value) {
    return new DrawId(value);
  }

  /** Return DrawId or null if id is null */
  public static DrawId nullableOf(UUID id) {
    return id == null ? null : new DrawId(id);
  }

  /**
   * Static factory from String representation of UUID.
   *
   * @throws IllegalArgumentException if the string is not a valid UUID
   */
  public static DrawId of(String id) {
    if (id == null) throw new IllegalArgumentException("draw id string is required");
    return new DrawId(UUID.fromString(id));
  }

  public static DrawId random() {
    return new DrawId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() {
    return value;
  }
}
