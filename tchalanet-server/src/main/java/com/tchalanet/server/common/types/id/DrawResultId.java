package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Identifiant pour DrawResult (wrapper autour de UUID). */
public record DrawResultId(UUID value) {
  public DrawResultId {
    if (value == null) throw new IllegalArgumentException("DrawResultId.value is null");
  }

  public static DrawResultId of(UUID value) {
    return new DrawResultId(value);
  }

  /** Return DrawResultId or null if id is null */
  public static DrawResultId nullableOf(UUID id) {
    return id == null ? null : new DrawResultId(id);
  }

  public static DrawResultId of(String id) {
    if (id == null) throw new IllegalArgumentException("drawResult id string is required");
    return new DrawResultId(UUID.fromString(id));
  }

  public static DrawResultId random() {
    return new DrawResultId(UUID.randomUUID());
  }

  /** Return the underlying UUID value */
  public UUID uuid() {
    return value;
  }

  public UUID value() {
    return value;
  }
}
