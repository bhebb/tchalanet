package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record EventId(UUID value) {
  public static EventId of(UUID u) { return new EventId(u); }
}
