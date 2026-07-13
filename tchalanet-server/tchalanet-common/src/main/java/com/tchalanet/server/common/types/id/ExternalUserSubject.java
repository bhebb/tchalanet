package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for an external identity provider subject. */
public record ExternalUserSubject(UUID value) {

  public ExternalUserSubject {
    if (value == null) throw new IllegalArgumentException("ExternalUserSubject.value is null");
  }

  public static ExternalUserSubject of(UUID value) {
    return new ExternalUserSubject(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static ExternalUserSubject nullableOf(UUID raw) {
    return raw == null ? null : new ExternalUserSubject(raw);
  }

  /** Parse from UUID string (web/input). */
  public static ExternalUserSubject parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("ExternalUserSubject string is required");
    }
    return new ExternalUserSubject(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
