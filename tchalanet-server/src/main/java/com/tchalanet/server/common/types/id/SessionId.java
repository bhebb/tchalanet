package com.tchalanet.server.common.types.id;

import java.util.Objects;
import java.util.UUID;

/** Value object identifier for Session. */
public record SessionId(UUID value) {

  public SessionId {
    if (value == null) throw new IllegalArgumentException("SessionId.value is null");
  }

  public static SessionId of(UUID value) {
    return new SessionId(Objects.requireNonNull(value, "session id is required"));
  }

  /** Return SessionId or null if id is null */
  public static SessionId nullableOf(UUID id) {
    return id == null ? null : new SessionId(id);
  }

  public static SessionId of(String id) {
    if (id == null) throw new IllegalArgumentException("session id string is required");
    return new SessionId(UUID.fromString(id));
  }

  public static SessionId random() {
    return new SessionId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() {
    return value;
  }
}
