package com.tchalanet.server.features.pagemodel;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LangResolver {

  public String resolve(LangResolverContext ctx) {
    List<String> available = ctx.availableLangs();

    // 1) URL
    Optional<String> candidate = ctx.langFromUrl().filter(l -> isSupported(l, available));
    if (candidate.isPresent()) {
      return candidate.get();
    }

    // 2) user preference
    candidate = ctx.userPreferredLang().filter(l -> isSupported(l, available));
    if (candidate.isPresent()) {
      return candidate.get();
    }

    // 3) tenant default
    candidate = ctx.tenantDefaultLang().filter(l -> isSupported(l, available));
    if (candidate.isPresent()) {
      return candidate.get();
    }

    // 4) meta.default_lang
    candidate = ctx.metaDefaultLang().filter(l -> isSupported(l, available));
    if (candidate.isPresent()) {
      return candidate.get();
    }

    // 5) meta.langs[0]
    if (!available.isEmpty()) {
      return available.get(0);
    }

    // 6) fallback global
    return ctx.globalFallbackLang();
  }

  private boolean isSupported(String lang, List<String> available) {
    if (available == null || available.isEmpty() || lang == null) {
      return false;
    }
    String normalized = normalize(lang);
    return available.stream().map(this::normalize).anyMatch(normalized::equals);
  }

  private String normalize(String lang) {
    return Locale.forLanguageTag(lang).getLanguage().toLowerCase(Locale.ROOT);
  }

  public record LangResolverContext(
      Optional<String> langFromUrl,
      Optional<String> userPreferredLang,
      Optional<String> tenantDefaultLang,
      Optional<String> metaDefaultLang,
      List<String> availableLangs,
      String globalFallbackLang) {}
}
