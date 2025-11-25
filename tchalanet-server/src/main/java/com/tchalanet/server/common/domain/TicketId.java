package com.tchalanet.server.common.domain;

import java.util.UUID;

public record TicketId(UUID value) {
  public static TicketId of(UUID id) {
    return id == null ? null : new TicketId(id);
  }
}
