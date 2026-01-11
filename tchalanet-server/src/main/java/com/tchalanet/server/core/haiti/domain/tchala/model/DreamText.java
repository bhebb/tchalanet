package com.tchalanet.server.core.haiti.domain.tchala.model;

import com.tchalanet.server.core.haiti.domain.tchala.exception.InvalidTchalaEntryException;
import java.text.Normalizer;
import java.util.Locale;

public record DreamText(String value) {
  public static DreamText of(String raw) {
    if (raw == null) throw new InvalidTchalaEntryException("dream is null");
    String t = raw.trim();
    if (t.isBlank()) throw new InvalidTchalaEntryException("dream is blank");
    if (t.length() > 120) throw new InvalidTchalaEntryException("dream too long");
    return new DreamText(t);
  }

  public String normalizedForKey() {
    String s = value.toLowerCase(Locale.ROOT);
    s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    s = s.replaceAll("[^a-z0-9]+", " ");
    s = s.trim().replaceAll("\\s+", " ");
    return s;
  }
}
