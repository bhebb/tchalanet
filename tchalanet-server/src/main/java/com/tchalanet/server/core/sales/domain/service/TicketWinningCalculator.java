package com.tchalanet.server.core.sales.domain.service;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class TicketWinningCalculator {

    private static final Pattern SEP = Pattern.compile("[,\\s\\-]+");

    public BigDecimal calculateWinningAmount(Ticket ticket, DrawResultMatchView result) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(result, "result");

        BigDecimal total = BigDecimal.ZERO;

        Set<String> twoDigits = new HashSet<>(result.twoDigits());

        for (TicketLine line : ticket.getLines()) {
            if (isWinningLine(line, result, twoDigits)) {
                total = total.add(zeroIfNull(line.potentialPayout()));
            }
        }
        return total;
    }

    private boolean isWinningLine(TicketLine line, DrawResultMatchView result, Set<String> twoDigits) {
        BetType betType = line.betType();
        String selection = safe(line.selection());

        return switch (betType) {
            case MATCH_1_2D -> match1_2d(selection, twoDigits);
            case MATCH_2_2D -> matchN_2d(selection, twoDigits, 2);
            case MATCH_3_2D -> matchN_2d(selection, twoDigits, 3);
            case MARRIAGE_2D2D -> marriage2d2d(selection, twoDigits);
            case LOTTO3_3D -> exactDigits(selection, result.lot4(), 3);
            case LOTTO4_PATTERN -> lotto4(selection, line.betOption() == null ? null : line.betOption(), result, betType.canonicalWidth());
            case LOTTO5_PATTERN -> lotto5(selection, line.betOption() == null ? null : line.betOption(), result, betType.canonicalWidth());
        };
    }

    // ---------------- BASIC BET RULES ----------------

    private boolean match1_2d(String selection, Set<String> twoDigits) {
        return selection.length() == 2 && twoDigits.contains(selection);
    }

    private boolean matchN_2d(String selection, Set<String> twoDigits, int expectedCount) {
        List<String> parts = split(selection);
        if (parts.size() != expectedCount) return false;
        for (String p : parts) {
            if (p.length() != 2) return false;
            if (!twoDigits.contains(p)) return false;
        }
        return true;
    }

    private boolean marriage2d2d(String selection, Set<String> twoDigits) {
        List<String> parts = split(selection);
        if (parts.size() != 2) return false;
        String a = parts.get(0);
        String b = parts.get(1);
        if (a.length() != 2 || b.length() != 2) return false;
        return twoDigits.contains(a) && twoDigits.contains(b);
    }

    private boolean exactDigits(String selection, String drawn, int len) {
        if (drawn == null) return false;
        return selection.length() == len && selection.equals(drawn);
    }

    // ---------------- LOTTO4 / LOTTO5 ----------------

    private boolean lotto4(String selection, Short option, DrawResultMatchView r, int expectedLen) {
        if (selection.length() != expectedLen) return false;
        if (option == null) return false;

        String ab = selection.substring(0, 2);
        String cd = selection.substring(2, 4);

        return switch (option) {
            case 1 -> ab.equals(r.lot2()) && cd.equals(r.lot3());
            case 2 -> ab.equals(r.lot1()) && cd.equals(r.lot2());
            case 3 -> ab.equals(r.lot1()) && cd.equals(r.lot3());
            default -> false;
        };
    }

    private boolean lotto5(String selection, Short option, DrawResultMatchView r, int expectedLen) {
        if (selection.length() != expectedLen) return false;
        if (option == null) return false;

        String pick3 = r.lot4();
        if (pick3 == null || pick3.length() != 3) return false;

        String abc = selection.substring(0, 3);
        String de = selection.substring(3, 5);

        return switch (option) {
            case 1 -> abc.equals(pick3) && de.equals(r.lot2());
            case 2 -> abc.equals(pick3) && de.equals(r.lot3());
            case 3 -> throw new UnsupportedOperationException("LOTTO5_PATTERN option 3 not yet implemented");
            default -> false;
        };
    }

    // ---------------- UTIL ----------------

    private List<String> split(String selection) {
        if (selection == null || selection.isBlank()) return List.of();
        String[] raw = SEP.split(selection.trim());
        List<String> out = new ArrayList<>();
        for (String r : raw) {
            if (r == null || r.isBlank()) continue;
            out.add(r.trim());
        }
        return out;
    }

    private BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
