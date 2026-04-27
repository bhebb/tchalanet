package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Keycloak user (external subject id). */
public record KeycloakUserSub(UUID value) {

  public KeycloakUserSub {
    if (value == null) throw new IllegalArgumentException("KeycloakUserId.value is null");
  }

  public static KeycloakUserSub of(UUID value) {
    return new KeycloakUserSub(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static KeycloakUserSub nullableOf(UUID raw) {
    return raw == null ? null : new KeycloakUserSub(raw);
  }

  /** Parse from UUID string (web/input). */
  public static KeycloakUserSub parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("KeycloakUserId string is required");
    }
    return new KeycloakUserSub(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
