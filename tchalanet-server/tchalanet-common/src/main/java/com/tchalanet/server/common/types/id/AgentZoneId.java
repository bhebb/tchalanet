package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record AgentZoneId(UUID value) {
  public AgentZoneId {
    if (value == null) {
      throw new IllegalArgumentException("AgentZoneId.value is null");
    }
  }

  public static AgentZoneId of(UUID value) { return new AgentZoneId(value); }
  public static AgentZoneId nullableOf(UUID raw) { return raw == null ? null : new AgentZoneId(raw); }
  public static AgentZoneId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("AgentZoneId string is required");
    return new AgentZoneId(UUID.fromString(raw));
  }
}
