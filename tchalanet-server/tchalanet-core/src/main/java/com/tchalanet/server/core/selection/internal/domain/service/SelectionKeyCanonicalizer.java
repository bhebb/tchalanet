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
    private static final Pattern LOTTO4_FRONT_PAIR_CANONICAL = Pattern.compile("^\\d{2}\\*\\*$");
    private static final Pattern LOTTO4_BACK_PAIR_CANONICAL = Pattern.compile("^\\*\\*\\d{2}$");
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
                canonicalize2dPair(BetOption.from(betType, rawBetOption), s);

            case LOTTO3_3D ->
                canonicalizeDigits(BetOption.from(betType, rawBetOption), s, 3);

            case LOTTO4_PATTERN ->
                canonicalizeLotto4(rawBetOption, s);

            case LOTTO5_PATTERN ->
                canonicalizeDigits(BetOption.from(betType, rawBetOption), s, 5);
        };

        return SelectionKey.of(canonical);
    }

    private static String canonicalizeLotto4(Short rawOption, String s) {
        BetOption option = BetOption.from(BetType.LOTTO4_PATTERN, rawOption);

        assert option != null;
        return switch (option) {
            case LOTTO4_STRAIGHT, LOTTO4_BOX ->
                canonicalizeExactDigits(s, 4);

            case LOTTO4_FRONT_PAIR ->
                canonicalizeLotto4FrontPair(s);

            case LOTTO4_BACK_PAIR ->
                canonicalizeLotto4BackPair(s);

            default ->
                throw new IllegalArgumentException("Unsupported LOTTO4 option: " + option);
        };
    }

    private static String canonicalizeLotto4FrontPair(String s) {
        if (LOTTO4_FRONT_PAIR_CANONICAL.matcher(s).matches()) {
            return s;
        }
        return canonicalizeExactDigits(s, 2) + "**";
    }

    private static String canonicalizeLotto4BackPair(String s) {
        if (LOTTO4_BACK_PAIR_CANONICAL.matcher(s).matches()) {
            return s;
        }
        return "**" + canonicalizeExactDigits(s, 2);
    }
    public static SelectionKey canonicalize(BetType betType, String rawSelectionKey) {
        return canonicalize(betType, null, rawSelectionKey);
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

    private static String canonicalizeDigits(BetOption ignored, String s, int width) {
        if (width == 5) {
            return canonicalizeExactDigits(s, width);
        }
        return canonicalizeDigitsStrict(s, width);
    }

    private static String canonicalizeExactDigits(String s, int width) {
        String digits = s.trim();
        if (!DIGITS.matcher(digits).matches()) {
            throw new IllegalArgumentException("selection must be numeric: " + s);
        }
        if (digits.length() != width) {
            throw new IllegalArgumentException("selection must be " + width + " digits: " + s);
        }
        return digits;
    }

    private static String canonicalize2dPair(BetOption ignored, String s) {
        String cleaned = s.replace('/', '-').replace(' ', '-');
        String[] p = cleaned.split("-");
        if (p.length != 2) {
            throw new IllegalArgumentException("invalid 2D pair selection: " + s);
        }

        String a = canonicalizeDigitsStrict(p[0], 2);
        String b = canonicalizeDigitsStrict(p[1], 2);

        return a + "-" + b;
    }
}
