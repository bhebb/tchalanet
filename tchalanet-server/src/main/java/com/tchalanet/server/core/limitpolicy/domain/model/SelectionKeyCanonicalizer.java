package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.core.sales.domain.model.BetType;

public class SelectionKeyCanonicalizer {

  public static String canonicalize(BetType betType, String rawSelection) {
    return switch (betType) {
      case MATCH_1_2D, MATCH_2_2D, MATCH_3_2D -> canonicalize2D(rawSelection);
      case LOTTO3_3D -> canonicalize3D(rawSelection);
      case MARRIAGE_2D2D -> canonicalizeMarriage(rawSelection);
      case LOTTO4_PATTERN, LOTTO5_PATTERN -> canonicalizePattern(rawSelection);
    };
  }

  private static String canonicalize2D(String selection) {
    if (selection.length() == 1) return "0" + selection;
    if (selection.length() == 2) return selection;
    throw new IllegalArgumentException("Invalid 2D selection: " + selection);
  }

  private static String canonicalize3D(String selection) {
    if (selection.length() == 1) return "00" + selection;
    if (selection.length() == 2) return "0" + selection;
    if (selection.length() == 3) return selection;
    throw new IllegalArgumentException("Invalid 3D selection: " + selection);
  }

  private static String canonicalizeMarriage(String selection) {
    String[] parts = selection.split("-");
    if (parts.length != 2) throw new IllegalArgumentException("Invalid marriage: " + selection);
    String a = canonicalize2D(parts[0]);
    String b = canonicalize2D(parts[1]);
    return a.compareTo(b) < 0 ? a + "-" + b : b + "-" + a; // sort min-max
  }

  private static String canonicalizePattern(String selection) {
    // Assume format is already <pattern>:<digits>
    return selection;
  }
}
