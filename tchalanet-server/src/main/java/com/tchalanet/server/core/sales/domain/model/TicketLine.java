package com.tchalanet.server.core.sales.domain.model;

import com.tchalanet.server.common.types.enums.BetType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record TicketLine(
    String gameCode,
    String selection,           // MUST be normalized before creating the line (via BetSelectionNormalizer)
    BigDecimal stake,
    BigDecimal oddsSnapshot,
    BigDecimal potentialPayout,
    BetType betType,
    Short betOption           // nullable; required for LOTTO4_PATTERN/LOTTO5_PATTERN (1..3)
) {

    public TicketLine {
        gameCode = requireNonBlank(gameCode, "gameCode");
        selection = requireNonBlank(selection, "selection");
        Objects.requireNonNull(betType, "betType");

        stake = money2(stake, "stake");
        if (stake.signum() <= 0) throw new IllegalArgumentException("stake must be > 0");

        oddsSnapshot = scale(oddsSnapshot, 4, "oddsSnapshot");
        if (oddsSnapshot.signum() <= 0) throw new IllegalArgumentException("oddsSnapshot must be > 0");

        potentialPayout = money2(potentialPayout, "potentialPayout");
        if (potentialPayout.signum() < 0) throw new IllegalArgumentException("potentialPayout cannot be negative");

        // --- betOption invariants
        boolean isPattern = betType == BetType.LOTTO4_PATTERN || betType == BetType.LOTTO5_PATTERN;
        if (isPattern) {
            if (betOption == null) {
                throw new IllegalArgumentException("betOption is required for " + betType + " (1..3)");
            }
            if (betOption < 1 || betOption > 3) {
                throw new IllegalArgumentException("betOption must be 1..3 for " + betType + " but was " + betOption);
            }
        } else {
            if (betOption != null) {
                throw new IllegalArgumentException("betOption must be null for betType=" + betType);
            }
        }

        // Optional consistency check: potentialPayout ~= stake * oddsSnapshot (rounded to 2)
        // (You can relax/remove this if you later add fees or different payout rules.)
        BigDecimal expected = stake.multiply(oddsSnapshot).setScale(2, RoundingMode.HALF_UP);
        if (potentialPayout.compareTo(expected) != 0) {
            throw new IllegalArgumentException(
                "potentialPayout mismatch. expected=" + expected + " but was=" + potentialPayout);
        }
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return v;
    }

    private static BigDecimal scale(BigDecimal v, int scale, String field) {
        Objects.requireNonNull(v, field + " is required");
        return v.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Money with max 2 decimals (accepts "10" or "10.5" or "10.50").
     */
    private static BigDecimal money2(BigDecimal v, String field) {
        Objects.requireNonNull(v, field + " is required");
        if (v.scale() > 2) {
            throw new IllegalArgumentException(field + " must have max 2 decimals. value=" + v);
        }
        return v.setScale(2, RoundingMode.UNNECESSARY); // ensures canonical 2-decimal storage
    }
}
