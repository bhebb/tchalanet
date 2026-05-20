package com.tchalanet.server.core.sales.internal.application.service.result;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultProjection;
import com.tchalanet.server.core.sales.api.model.line.TicketLineResult;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class TicketWinningCalculator {

    private static final Pattern SEP = Pattern.compile("[,\\s\\-]+");

    public HashMap<TicketLineId, TicketLineResult> computeLineResults(
        Ticket ticket,
        DrawResultProjection projection
    ) {
        var twoDigits = extractTwoDigits(projection);
        var pick3 = normalizeDigits(projection.lot4(), 3);

        var results = new HashMap<TicketLineId, TicketLineResult>();
        var zero = Money.zero(ticket.money().currency());

        for (TicketLine line : ticket.lines()) {
            boolean won = isWinningLine(line, twoDigits, pick3);
            results.put(line.id(), new TicketLineResult(
                won ? TicketLineResultStatus.WON : TicketLineResultStatus.LOST,
                won ? line.potentialPayoutAmount() : zero
            ));
        }

        return results;
    }

    private boolean isWinningLine(TicketLine line, Set<String> twoDigits, String pick3) {
        var selection = safe(line.selection().key().value());
        BetType betType = line.betType();

        return switch (betType) {
            case MATCH_1_2D -> match1_2d(selection, twoDigits);
            case MATCH_2_2D -> matchN_2d(selection, twoDigits, 2);
            case MATCH_3_2D -> matchN_2d(selection, twoDigits, 3);
            case MARRIAGE_2D2D -> marriage2d2d(selection, twoDigits);
            case LOTTO3_3D -> exactDigits(selection, pick3);
            case LOTTO4_PATTERN -> lotto4(selection, line.betOption(), twoDigits);
            case LOTTO5_PATTERN -> lotto5(selection, line.betOption(), pick3, twoDigits);
        };
    }

    private Set<String> extractTwoDigits(DrawResultProjection projection) {
        var out = new HashSet<String>();
        add2d(out, projection.lot1());
        add2d(out, projection.lot2());
        add2d(out, projection.lot3());
        if (projection.derivedPairs() != null) {
            projection.derivedPairs().forEach(v -> add2d(out, v));
        }
        return out;
    }

    private void add2d(Set<String> out, String value) {
        var normalized = normalizeDigits(value, 2);
        if (normalized != null) {
            out.add(normalized);
        }
    }

    private boolean match1_2d(String selection, Set<String> twoDigits) {
        var s = normalizeDigits(selection, 2);
        return s != null && twoDigits.contains(s);
    }

    private boolean matchN_2d(String selection, Set<String> twoDigits, int expectedCount) {
        var parts = split(selection);
        if (parts.size() != expectedCount) {
            return false;
        }
        for (String part : parts) {
            var n = normalizeDigits(part, 2);
            if (n == null || !twoDigits.contains(n)) {
                return false;
            }
        }
        return true;
    }

    private boolean marriage2d2d(String selection, Set<String> twoDigits) {
        var parts = split(selection);
        if (parts.size() != 2) {
            return false;
        }
        var a = normalizeDigits(parts.get(0), 2);
        var b = normalizeDigits(parts.get(1), 2);
        return a != null && b != null && twoDigits.contains(a) && twoDigits.contains(b);
    }

    private boolean exactDigits(String selection, String drawn) {
        var s = normalizeDigits(selection, 3);
        return s != null && s.equals(drawn);
    }

    private boolean lotto4(String selection, Short option, Set<String> twoDigits) {
        var s = digitsOnly(selection);
        if (s.length() != 4 || option == null) {
            return false;
        }
        String ab = s.substring(0, 2);
        String cd = s.substring(2, 4);

        var pairs = new ArrayList<>(twoDigits);
        if (pairs.size() < 3) {
            return false;
        }

        return switch (option) {
            case 1 -> pairs.contains(ab) && pairs.contains(cd);
            case 2 -> pairs.contains(ab) && pairs.contains(cd);
            case 3 -> pairs.contains(ab) && pairs.contains(cd);
            default -> false;
        };
    }

    private boolean lotto5(String selection, Short option, String pick3, Set<String> twoDigits) {
        var s = digitsOnly(selection);
        if (s.length() != 5 || option == null || pick3 == null) {
            return false;
        }
        String abc = s.substring(0, 3);
        String de = s.substring(3, 5);

        return switch (option) {
            case 1, 2 -> abc.equals(pick3) && twoDigits.contains(de);
            case 3 -> false;
            default -> false;
        };
    }

    private List<String> split(String selection) {
        if (selection == null || selection.isBlank()) {
            return List.of();
        }
        String[] raw = SEP.split(selection.trim());
        var out = new ArrayList<String>();
        for (String part : raw) {
            if (part != null && !part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private static String normalizeDigits(String value, int expectedLen) {
        var digits = digitsOnly(value);
        if (digits.length() != expectedLen) {
            return null;
        }
        return digits;
    }

    private static String digitsOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\D", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
