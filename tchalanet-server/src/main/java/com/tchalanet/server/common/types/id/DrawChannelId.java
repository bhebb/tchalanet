package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record DrawChannelId(UUID value) {
  public DrawChannelId {
    if (value == null) {
      throw new IllegalArgumentException("DrawChannelId cannot be null");
    }
  }

  public static DrawChannelId of(UUID value) {
    return new DrawChannelId(value);
  }

  /** Return DrawChannelId or null if id is null */
  public static DrawChannelId nullableOf(UUID id) {
    return id == null ? null : new DrawChannelId(id);
  }

  public static DrawChannelId of(String id) {
    if (id == null) throw new IllegalArgumentException("draw channel id string is required");
    return new DrawChannelId(UUID.fromString(id));
  }

  public static DrawChannelId random() {
    return new DrawChannelId(UUID.randomUUID());
  }

  // keep old name for backward compatibility
  public static DrawChannelId generate() {
    return random();
  }

  /** Return the underlying UUID value */
  public UUID uuid() {
    return value;
  }

  public UUID value() {
    return value;
  }
}
