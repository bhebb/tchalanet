package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record GameId(UUID value) {
  public GameId {
    if (value == null) throw new IllegalArgumentException("GameId.value is null");
  }

  public static GameId of(UUID value) {
    return new GameId(value);
  }

  /** Return GameId or null if id is null */
  public static GameId nullableOf(UUID id) {
    return id == null ? null : new GameId(id);
  }

  public static GameId of(String id) {
    if (id == null) throw new IllegalArgumentException("game id string is required");
    return new GameId(UUID.fromString(id));
  }

  public static GameId random() {
    return new GameId(UUID.randomUUID());
  }

  /** Return the underlying UUID value */
  public UUID uuid() {
    return value;
  }

  public UUID value() {
    return value;
  }
}
