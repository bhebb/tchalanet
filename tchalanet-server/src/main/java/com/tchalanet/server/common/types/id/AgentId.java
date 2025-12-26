package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Value object identifier for Agent. */
public record AgentId(UUID value) {

  public AgentId {
    if (value == null) throw new IllegalArgumentException("AgentId.value is null");
  }

  /** Static factory from UUID. */
  public static AgentId of(UUID value) {
    return new AgentId(value);
  }

  /** Return AgentId or null if id is null */
  public static AgentId nullableOf(UUID id) {
    return id == null ? null : new AgentId(id);
  }

  /**
   * Static factory from String representation of UUID.
   *
   * @throws IllegalArgumentException if the string is not a valid UUID
   */
  public static AgentId of(String id) {
    if (id == null) throw new IllegalArgumentException("agent id string is required");
    return new AgentId(UUID.fromString(id));
  }

  public static AgentId random() {
    return new AgentId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }

  public UUID uuid() {
    return value;
  }
}
