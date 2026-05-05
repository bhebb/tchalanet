package com.tchalanet.server.core.haiti.domain.tchala.model;

import java.util.Objects;

public record DedupeKey(TchalaLang lang, String key) {
  public static DedupeKey from(TchalaLang lang, DreamText dream) {
    Objects.requireNonNull(lang);
    Objects.requireNonNull(dream);
    String normalizedKey = dream.normalizedForKey();
    if (normalizedKey.isBlank()) throw new IllegalArgumentException("dedupe slotKey blank");
    return new DedupeKey(lang, normalizedKey);
  }
}
