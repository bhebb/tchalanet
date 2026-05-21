package com.tchalanet.server.core.selection.internal.domain.service;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.selection.api.model.SelectionKey;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Pure, deterministic selection canonicalization.
 */
public final class SelectionKeyCanonicalizer {

    private static final Pattern DIGITS = Pattern.compile("^\\d+$");
    private static final Pattern MASK_4 = Pattern.compile("^[0-9*]{4}$");
    private static final Pattern MASK_5 = Pattern.compile("^[0-9*]{5}$");

    private SelectionKeyCanonicalizer() {
    }

    public static SelectionKey canonicalize(
        BetType betType,
        Short rawBetOption,
        String rawSelectionKey
    ) {
        Objects.requireNonNull(betType, "betType");
        Objects.requireNonNull(rawSelectionKey, "selectionKey");

        String s = rawSelectionKey.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("selectionKey cannot be blank");
        }

        String canonical = switch (betType) {
            case MATCH_1_2D, MATCH_2_2D, MATCH_3_2D ->
                canonicalizeDigitsStrict(s, 2);

            case MARRIAGE_2D2D ->
                canonicalize2dPair(s, false);

            case LOTTO3_3D ->
                canonicalizeDigitsStrict(s, 3);

            case LOTTO4_PATTERN ->
                canonicalizeLotto4(rawBetOption, s);

            case LOTTO5_PATTERN ->
                canonicalizeDigitsStrict(s, 5);
        };

        return SelectionKey.of(canonical);
    }

    private static String canonicalizeLotto4(Short rawOption, String s) {
        BetOption option = BetOption.from(BetType.LOTTO4_PATTERN, rawOption);

        assert option != null;
        return switch (option) {
            case LOTTO4_STRAIGHT, LOTTO4_BOX ->
                canonicalizeDigitsStrict(s, 4);

            case LOTTO4_FRONT_PAIR ->
                canonicalizeDigitsStrict(s, 2) + "**";

            case LOTTO4_BACK_PAIR ->
                "**" + canonicalizeDigitsStrict(s, 2);

            default ->
                throw new IllegalArgumentException("Unsupported LOTTO4 option: " + option);
        };
    }
    public static SelectionKey canonicalize(BetType betType, String rawSelectionKey) {
        Objects.requireNonNull(betType, "betType");
        Objects.requireNonNull(rawSelectionKey, "selectionKey");

        String s = rawSelectionKey.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("selectionKey cannot be blank");
        }

        String canonical = switch (betType) {
            case MATCH_1_2D, LOTTO3_3D -> canonicalizeDigitsStrict(s, betType.canonicalWidth());
            case MATCH_2_2D -> canonicalize2dPair(s, false);
            case MATCH_3_2D -> canonicalize3x2d(s);
            case MARRIAGE_2D2D -> canonicalize2dPair(s, true);
            case LOTTO4_PATTERN, LOTTO5_PATTERN -> canonicalizePatternStrict(s, betType.canonicalWidth());
        };

        return SelectionKey.of(canonical);
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
        String cleaned = s.replace('/', '-').replace(' ', '-');
        String[] p = cleaned.split("-");
        if (p.length != 2) {
            throw new IllegalArgumentException("invalid 2D pair selection: " + s);
        }

        String a = canonicalizeDigitsStrict(p[0], 2);
        String b = canonicalizeDigitsStrict(p[1], 2);

        if (sortPair && a.compareTo(b) > 0) {
            String tmp = a;
            a = b;
            b = tmp;
        }
        return a + "-" + b;
    }

    private static String canonicalize3x2d(String s) {
        String cleaned = s.replace('/', '-').replace(' ', '-');
        String[] p = cleaned.split("-");
        if (p.length != 3) {
            throw new IllegalArgumentException("invalid 3x2D selection: " + s);
        }

        return canonicalizeDigitsStrict(p[0], 2)
            + "-"
            + canonicalizeDigitsStrict(p[1], 2)
            + "-"
            + canonicalizeDigitsStrict(p[2], 2);
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
