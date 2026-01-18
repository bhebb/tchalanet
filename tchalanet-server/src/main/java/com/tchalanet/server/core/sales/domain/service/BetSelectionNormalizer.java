package com.tchalanet.server.core.sales.domain.service;

import com.tchalanet.server.common.types.enums.BetType;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class BetSelectionNormalizer {

    private static final Pattern MASK_4 = Pattern.compile("^[0-9*]{4}$");
    private static final Pattern MASK_5 = Pattern.compile("^[0-9*]{5}$");

    public String normalize(BetType betType, String rawSelection) {
        Objects.requireNonNull(betType, "betType");
        Objects.requireNonNull(rawSelection, "rawSelection");

        String s = rawSelection.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("selection cannot be blank");

        return switch (betType) {
            case MATCH_1_2D -> padDigitsOnly(s, 2);

            case MATCH_2_2D, MARRIAGE_2D2D -> normalize2dPair(s);

            case MATCH_3_2D -> normalize3x2d(s);

            case LOTTO3_3D -> padDigitsOnly(s, 3);

            case LOTTO4_PATTERN -> normalizeMask(s, 4, MASK_4);

            case LOTTO5_PATTERN -> normalizeMask(s, 5, MASK_5);
        };
    }

    private String normalize2dPair(String s) {
        // Accept "12-34" or "12 34" or "12/34" -> canonical "12-34"
        String cleaned = s.replace('/', '-').replace(' ', '-');
        String[] p = cleaned.split("-");
        if (p.length != 2) throw new IllegalArgumentException("Invalid 2D pair selection: " + s);
        String a = padDigitsOnly(p[0], 2);
        String b = padDigitsOnly(p[1], 2);
        return a + "-" + b;
    }

    private String normalize3x2d(String s) {
        String cleaned = s.replace('/', '-').replace(' ', '-');
        String[] p = cleaned.split("-");
        if (p.length != 3) throw new IllegalArgumentException("Invalid 3x2D selection: " + s);
        return padDigitsOnly(p[0], 2) + "-" + padDigitsOnly(p[1], 2) + "-" + padDigitsOnly(p[2], 2);
    }

    private String normalizeMask(String s, int expectedLen, Pattern regex) {
        // remove spaces, enforce length, allow only digits or '*'
        String m = s.replace(" ", "");
        if (m.length() != expectedLen) {
            throw new IllegalArgumentException("Mask must be length " + expectedLen + ": " + s);
        }
        if (!regex.matcher(m).matches()) {
            throw new IllegalArgumentException("Invalid mask pattern: " + s + " (allowed: digits and *)");
        }
        return m;
    }

    private String padDigitsOnly(String s, int len) {
        String digits = s.trim();
        if (!digits.matches("^\\d+$")) {
            throw new IllegalArgumentException("Selection must be numeric: " + s);
        }
        if (digits.length() > len) {
            throw new IllegalArgumentException("Selection too long (" + len + " digits max): " + s);
        }
        return "0".repeat(len - digits.length()) + digits;
    }
}
