package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Value object identifier for Terminal. */
public record TerminalId(UUID value) {

  public TerminalId {
    if (value == null) throw new IllegalArgumentException("TerminalId.value is null");
  }

  /**
   * Static factory from UUID.
   */
  public static TerminalId of(UUID value) {
    return new TerminalId(value);
  }

  /** Return TerminalId or null if id is null */
  public static TerminalId nullableOf(UUID id) { return id == null ? null : new TerminalId(id); }

  /**
   * Static factory from String representation of UUID.
   * @throws IllegalArgumentException if the string is not a valid UUID
   */
  public static TerminalId of(String id) {
    if (id == null) throw new IllegalArgumentException("terminal id string is required");
    return new TerminalId(UUID.fromString(id));
  }

  public static TerminalId random() {
    return new TerminalId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() { return value; }
}
