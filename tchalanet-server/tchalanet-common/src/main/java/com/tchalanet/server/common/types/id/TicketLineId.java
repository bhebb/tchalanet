package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record TicketLineId(UUID value) {
  public TicketLineId {
    if (value == null) throw new IllegalArgumentException("TicketId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static TicketLineId of(UUID value) {
    return new TicketLineId(value);
  }

  public static TicketLineId nullableOf(UUID value) {
    return value == null ? null : new TicketLineId(value);
  }

  public static TicketLineId parse(String value) {
    return value == null ? null : new TicketLineId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
