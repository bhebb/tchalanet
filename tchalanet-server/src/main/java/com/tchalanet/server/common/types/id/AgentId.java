package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record AgentId(UUID value) {
  public AgentId {
    if (value == null) throw new IllegalArgumentException("AgentId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static AgentId of(UUID value) {
    return new AgentId(value);
  }

  public static AgentId nullableOf(UUID value) {
    return value == null ? null : new AgentId(value);
  }

  public static AgentId parse(String value) {
    return value == null ? null : new AgentId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
