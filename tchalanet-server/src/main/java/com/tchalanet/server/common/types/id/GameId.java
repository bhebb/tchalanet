package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Game. */
public record GameId(UUID value) {

  public GameId {
    if (value == null) throw new IllegalArgumentException("GameId.value is null");
  }

  public static GameId of(UUID value) {
    return new GameId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static GameId nullableOf(UUID raw) {
    return raw == null ? null : new GameId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static GameId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("GameId string is required");
    }
    return new GameId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
