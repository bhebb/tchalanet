package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record AgentId(UUID value) {
  public AgentId {
    if (value == null) {
      throw new IllegalArgumentException("AgentId.value is null");
    }
  }

  public static AgentId of(UUID value) { return new AgentId(value); }
  public static AgentId nullableOf(UUID raw) { return raw == null ? null : new AgentId(raw); }
  public static AgentId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("AgentId string is required");
    return new AgentId(UUID.fromString(raw));
  }
}
