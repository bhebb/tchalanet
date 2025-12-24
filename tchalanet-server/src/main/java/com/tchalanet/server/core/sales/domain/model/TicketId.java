package com.tchalanet.server.core.sales.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object for a Ticket's unique identifier. */
public record TicketId(UUID value) {
  public TicketId {
    Objects.requireNonNull(value, "TicketId value cannot be null");
  }

  public static TicketId of(UUID value) {
    return new TicketId(value);
  }
}
