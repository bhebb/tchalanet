package com.tchalanet.server.core.haiti.domain.tchala.model;

import java.util.Objects;

public record DedupeKey(TchalaLang lang, String key) {
  public static DedupeKey from(TchalaLang lang, DreamText dream) {
    Objects.requireNonNull(lang);
    Objects.requireNonNull(dream);
    String k = dream.normalizedForKey();
    if (k.isBlank()) throw new IllegalArgumentException("dedupe slotKey blank");
    return new DedupeKey(lang, k);
  }
}
