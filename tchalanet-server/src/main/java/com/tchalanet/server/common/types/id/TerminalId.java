package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Terminal. */
public record TerminalId(UUID value) {

  public TerminalId {
    if (value == null) throw new IllegalArgumentException("TerminalId.value is null");
  }

  public static TerminalId of(UUID value) {
    return new TerminalId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static TerminalId nullableOf(UUID raw) {
    return raw == null ? null : new TerminalId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static TerminalId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("TerminalId string is required");
    }
    return new TerminalId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
