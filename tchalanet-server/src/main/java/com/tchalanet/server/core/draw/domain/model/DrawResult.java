package com.tchalanet.server.core.draw.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DrawResult(
    DrawSource source,
    List<String> numbersMain,
    List<String> numbersExtra,
    Instant occurredAt,
    String rawPayload, // pour audit, peut être null
    boolean overridden,
    String overrideReason) {
  public DrawResult {
    Objects.requireNonNull(source);
    Objects.requireNonNull(numbersMain);
    Objects.requireNonNull(numbersExtra);
    Objects.requireNonNull(occurredAt);
    Objects.requireNonNull(rawPayload);
  }

  public DrawResult override(
      List<String> newNumbersMain, List<String> newNumbersExtra, String reason) {
    return new DrawResult(
        DrawSource.ADMIN_OVERRIDE,
        List.copyOf(newNumbersMain),
        newNumbersExtra != null ? List.copyOf(newNumbersExtra) : null,
        Instant.now(),
        this.rawPayload,
        true,
        reason);
  }
}
