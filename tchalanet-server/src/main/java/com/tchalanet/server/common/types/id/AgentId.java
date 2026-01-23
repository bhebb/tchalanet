package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Agent. */
public record AgentId(UUID value) {

  public AgentId {
    if (value == null) throw new IllegalArgumentException("AgentId.value is null");
  }

  public static AgentId of(UUID value) {
    return new AgentId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static AgentId nullableOf(UUID raw) {
    return raw == null ? null : new AgentId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static AgentId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("AgentId string is required");
    }
    return new AgentId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
