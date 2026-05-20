package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record TicketId(UUID value) {
  public TicketId {
    if (value == null) throw new IllegalArgumentException("TicketId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static TicketId of(UUID value) {
    return new TicketId(value);
  }

  public static TicketId nullableOf(UUID value) {
    return value == null ? null : new TicketId(value);
  }

  public static TicketId parse(String value) {
    return value == null ? null : new TicketId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
