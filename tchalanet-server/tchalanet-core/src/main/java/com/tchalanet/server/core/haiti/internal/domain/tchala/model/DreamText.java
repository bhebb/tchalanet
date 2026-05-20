package com.tchalanet.server.core.haiti.internal.domain.tchala.model;

import com.tchalanet.server.core.haiti.internal.domain.tchala.exception.InvalidTchalaEntryException;
import java.text.Normalizer;
import java.util.Locale;

public record DreamText(String value) {
  public static DreamText of(String raw) {
    if (raw == null) throw new InvalidTchalaEntryException("dream is null");
    String trimmed = raw.trim();
    if (trimmed.isBlank()) throw new InvalidTchalaEntryException("dream is blank");
    if (trimmed.length() > 120) throw new InvalidTchalaEntryException("dream too long");
    return new DreamText(trimmed);
  }

  public String normalizedForKey() {
    String normalized = value.toLowerCase(Locale.ROOT);
    normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    normalized = normalized.replaceAll("[^a-z0-9]+", " ");
    normalized = normalized.trim().replaceAll("\\s+", " ");
    return normalized;
  }
}
