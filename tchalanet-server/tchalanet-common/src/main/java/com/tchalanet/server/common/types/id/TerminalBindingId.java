package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record TerminalBindingId(UUID value) {
  public TerminalBindingId {
    if (value == null) {
      throw new IllegalArgumentException("TerminalBindingId.value is null");
    }
  }

  public static TerminalBindingId of(UUID value) {
    return new TerminalBindingId(value);
  }

  public static TerminalBindingId nullableOf(UUID raw) {
    return raw == null ? null : new TerminalBindingId(raw);
  }

  public static TerminalBindingId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("TerminalBindingId string is required");
    }
    return new TerminalBindingId(UUID.fromString(raw));
  }
}
