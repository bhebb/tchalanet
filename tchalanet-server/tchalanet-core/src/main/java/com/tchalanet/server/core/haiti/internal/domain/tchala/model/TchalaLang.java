package com.tchalanet.server.core.haiti.internal.domain.tchala.model;

import com.tchalanet.server.core.haiti.domain.tchala.exception.InvalidTchalaLangException;
import java.util.Locale;

public record TchalaLang(String value) {
  public static TchalaLang of(String raw) {
    if (raw == null) throw new InvalidTchalaLangException("lang is null");
    String normalizedLang = raw.trim().toLowerCase(Locale.ROOT);
    if (!(normalizedLang.equals("fr") || normalizedLang.equals("en") || normalizedLang.equals("ht")))
      throw new InvalidTchalaLangException("unsupported lang: " + raw);
    return new TchalaLang(normalizedLang);
  }
}
