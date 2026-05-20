package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import java.util.List;

final class PublicDrawResultQueryNormalizer {

  private PublicDrawResultQueryNormalizer() {}

  static List<String> slotKeys(List<String> slotKeys) {
    if (slotKeys == null || slotKeys.isEmpty()) {
      return List.of();
    }

    return slotKeys.stream()
        .filter(key -> key != null && !key.isBlank())
        .map(key -> key.trim().toUpperCase())
        .distinct()
        .toList();
  }

  static String provider(String provider) {
    if (provider == null || provider.isBlank()) {
      return null;
    }

    return provider.trim().toUpperCase();
  }
}
