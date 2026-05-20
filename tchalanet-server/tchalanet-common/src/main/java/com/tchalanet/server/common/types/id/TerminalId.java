package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record TerminalId(UUID value) {
  public TerminalId {
    if (value == null) throw new IllegalArgumentException("TerminalId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static TerminalId of(UUID value) {
    return new TerminalId(value);
  }

  public static TerminalId nullableOf(UUID value) {
    return value == null ? null : new TerminalId(value);
  }

  public static TerminalId parse(String value) {
    return value == null ? null : new TerminalId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
