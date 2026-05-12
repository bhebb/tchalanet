package com.tchalanet.server.common.selection;

import com.tchalanet.server.common.types.enums.BetType;

import java.util.Objects;
import java.util.regex.Pattern;

/** Pure, deterministic. No Spring, no DB. */
public final class SelectionKeyCanonicalizer {

  private SelectionKeyCanonicalizer() {}

  private static final Pattern DIGITS = Pattern.compile("^\\d+$");
  private static final Pattern MASK_4 = Pattern.compile("^[0-9*]{4}$");
  private static final Pattern MASK_5 = Pattern.compile("^[0-9*]{5}$");

  /**
   * Canonicalize a raw selection key according to the provided bet type.
   * This is a pure function (no side-effects).
   */
  public static String canonicalize(BetType betType, String rawSelectionKey) {
    Objects.requireNonNull(betType, "betType");
    Objects.requireNonNull(rawSelectionKey, "selectionKey");

    String s = rawSelectionKey.trim();
    if (s.isEmpty()) throw new IllegalArgumentException("selectionKey cannot be blank");

    return switch (betType) {
      case MATCH_1_2D -> canonicalizeDigitsStrict(s, betType.canonicalWidth());
      case LOTTO3_3D -> canonicalizeDigitsStrict(s, betType.canonicalWidth());

      case MATCH_2_2D -> canonicalize2dPair(s, false);   // keep order
      case MATCH_3_2D -> canonicalize3x2d(s);

      case MARRIAGE_2D2D -> canonicalize2dPair(s, true); // sort pair (commutative)

      case LOTTO4_PATTERN -> canonicalizePatternStrict(s, betType.canonicalWidth());
      case LOTTO5_PATTERN -> canonicalizePatternStrict(s, betType.canonicalWidth());

      default -> throw new IllegalStateException("Unsupported betType: " + betType);
    };
  }

  private static String canonicalizeDigitsStrict(String s, int width) {
    String digits = s.trim();
    if (!DIGITS.matcher(digits).matches()) {
      throw new IllegalArgumentException("selection must be numeric: " + s);
    }
    if (digits.length() > width) {
      throw new IllegalArgumentException("selection too long (" + width + " digits max): " + s);
    }
    return "0".repeat(width - digits.length()) + digits;
  }

  private static String canonicalize2dPair(String s, boolean sortPair) {
    // Accept "12-34" or "12 34" or "12/34" -> canonical "12-34"
    String cleaned = s.replace('/', '-').replace(' ', '-');
    String[] p = cleaned.split("-");
    if (p.length != 2) throw new IllegalArgumentException("invalid 2D pair selection: " + s);

    String a = canonicalizeDigitsStrict(p[0], 2);
    String b = canonicalizeDigitsStrict(p[1], 2);

    if (sortPair && a.compareTo(b) > 0) {
      String tmp = a; a = b; b = tmp;
    }
    return a + "-" + b;
  }

  private static String canonicalize3x2d(String s) {
    String cleaned = s.replace('/', '-').replace(' ', '-');
    String[] p = cleaned.split("-");
    if (p.length != 3) throw new IllegalArgumentException("invalid 3x2D selection: " + s);

    return canonicalizeDigitsStrict(p[0], 2)
        + "-" + canonicalizeDigitsStrict(p[1], 2)
        + "-" + canonicalizeDigitsStrict(p[2], 2);
  }

  private static String canonicalizePatternStrict(String s, int expectedLen) {
    String m = s.replace(" ", "");
    if (m.length() != expectedLen) {
      throw new IllegalArgumentException("mask must be length " + expectedLen + ": " + s);
    }
    Pattern regex = (expectedLen == 4) ? MASK_4 : MASK_5;
    if (!regex.matcher(m).matches()) {
      throw new IllegalArgumentException("invalid mask pattern (allowed digits and *): " + s);
    }
    return m;
  }
}
