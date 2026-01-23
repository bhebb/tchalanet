package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Ticket. */
public record TicketId(UUID value) {

  public TicketId {
    if (value == null) throw new IllegalArgumentException("TicketId.value is null");
  }

  public static TicketId of(UUID value) {
    return new TicketId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static TicketId nullableOf(UUID raw) {
    return raw == null ? null : new TicketId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static TicketId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("TicketId string is required");
    }
    return new TicketId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
