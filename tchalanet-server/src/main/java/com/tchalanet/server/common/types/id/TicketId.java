package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Value object identifier for Ticket. */
public record TicketId(UUID value) {

  public TicketId {
    if (value == null) throw new IllegalArgumentException("TicketId.value is null");
  }

  /**
   * Static factory from UUID.
   */
  public static TicketId of(UUID value) {
    return new TicketId(value);
  }

  /** Return TicketId or null if id is null */
  public static TicketId nullableOf(UUID id) { return id == null ? null : new TicketId(id); }

  /**
   * Static factory from String representation of UUID.
   * @throws IllegalArgumentException if the string is not a valid UUID
   */
  public static TicketId of(String id) {
    if (id == null) throw new IllegalArgumentException("ticket id string is required");
    return new TicketId(UUID.fromString(id));
  }

  public static TicketId random() {
    return new TicketId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() { return value; }
}
