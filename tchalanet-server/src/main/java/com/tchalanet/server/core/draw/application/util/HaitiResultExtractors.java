package com.tchalanet.server.core.draw.application.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public final class HaitiResultExtractors {
  private HaitiResultExtractors() {}

  /** Retourne [d1,d2,d3] si lot1 est "123" ou "1-2-3". Sinon empty. */
  public static List<Integer> lastPick3(JsonNode haitiResult) {
    if (haitiResult == null || haitiResult.isNull()) return List.of();

    // essaie plusieurs champs possibles
    String raw = text(haitiResult, "lot1");
    if (raw == null || raw.isBlank()) raw = text(haitiResult, "pick3");
    if (raw == null || raw.isBlank()) return List.of();

    // normalise: "1-2-3" -> "123", " 123 " -> "123"
    String digits = raw.replaceAll("[^0-9]", "");
    if (digits.length() != 3) return List.of();

    try {
      return List.of(
          Integer.parseInt(digits.substring(0, 1)),
          Integer.parseInt(digits.substring(1, 2)),
          Integer.parseInt(digits.substring(2, 3)));
    } catch (Exception e) {
      return List.of();
    }
  }

  private static String text(JsonNode obj, String field) {
    JsonNode n = obj.get(field);
    if (n == null || n.isNull()) return null;
    return n.isTextual() ? n.asText() : n.toString();
  }
}

