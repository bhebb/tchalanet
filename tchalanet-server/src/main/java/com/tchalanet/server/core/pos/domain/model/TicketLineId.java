package com.tchalanet.server.core.pos.domain.model;

import java.util.UUID;

public record TicketLineId(UUID value) {
  public static TicketLineId of(UUID v) {
    return v == null ? null : new TicketLineId(v);
  }
}
