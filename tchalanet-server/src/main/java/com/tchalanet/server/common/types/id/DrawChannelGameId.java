package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record DrawChannelGameId(UUID value) {
  public DrawChannelGameId {
    if (value == null) throw new IllegalArgumentException("DrawChannelGameId.value is null");
  }

  public static DrawChannelGameId of(UUID value) {
    return new DrawChannelGameId(value);
  }

  public static DrawChannelGameId nullableOf(UUID raw) {
    return raw == null ? null : new DrawChannelGameId(raw);
  }

  public static DrawChannelGameId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("DrawChannelGameId string is required");
    return new DrawChannelGameId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
