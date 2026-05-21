package com.tchalanet.server.core.sales.internal.application.service.result;

import com.tchalanet.server.catalog.game.api.model.BetOption;
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
import java.util.LinkedHashSet;
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
        var facts = TicketResultFacts.from(projection);

        var results = new HashMap<TicketLineId, TicketLineResult>();
        var zero = Money.zero(ticket.money().currency());

        for (TicketLine line : ticket.lines()) {
            boolean won = isWinningLine(line, facts);
            results.put(line.id(), new TicketLineResult(
                won ? TicketLineResultStatus.WON : TicketLineResultStatus.LOST,
                won ? line.potentialPayoutAmount() : zero
            ));
        }

        return results;
    }

    private boolean isWinningLine(TicketLine line, TicketResultFacts facts) {
        var selection = safe(line.selection().key().value());
        BetType betType = line.betType();

        return switch (betType) {
            case MATCH_1_2D, MATCH_2_2D, MATCH_3_2D -> match1_2d(selection, facts.twoDigits());
            case MARRIAGE_2D2D -> marriage2d2d(selection, line.betOption(), facts.orderedTwoDigits());
            case LOTTO3_3D -> lotto3(selection, line.betOption(), facts.pick3());
            case LOTTO4_PATTERN -> lotto4(selection, line.betOption(), facts.pick4());
            case LOTTO5_PATTERN -> lotto5(selection, line.betOption(), facts);
        };
    }

    private boolean match1_2d(String selection, Set<String> twoDigits) {
        var s = normalizeDigits(selection, 2);
        return s != null && twoDigits.contains(s);
    }

    private boolean marriage2d2d(String selection, Short rawOption, List<String> orderedTwoDigits) {
        var option = BetOption.from(BetType.MARRIAGE_2D2D, rawOption);
        var parts = split(selection);
        if (parts.size() != 2) {
            return false;
        }
        var a = normalizeDigits(parts.get(0), 2);
        var b = normalizeDigits(parts.get(1), 2);
        if (a == null || b == null) {
            return false;
        }
        return switch (option) {
            case MARRIAGE_EXACT_ORDER -> appearsInOrder(orderedTwoDigits, a, b);
            case MARRIAGE_REVERSE_ALLOWED -> orderedTwoDigits.contains(a) && orderedTwoDigits.contains(b);
            default -> false;
        };
    }

    private boolean lotto3(String selection, Short rawOption, String drawn) {
        var option = BetOption.from(BetType.LOTTO3_3D, rawOption);
        var s = normalizeDigits(selection, 3);
        if (s == null || drawn == null) {
            return false;
        }
        return switch (option) {
            case LOTTO3_STRAIGHT -> s.equals(drawn);
            case LOTTO3_BOX -> sortedDigits(s).equals(sortedDigits(drawn));
            default -> false;
        };
    }

    private boolean lotto4(String selection, Short rawOption, String pick4) {
        var option = BetOption.from(BetType.LOTTO4_PATTERN, rawOption);
        if (pick4 == null) {
            return false;
        }
        return switch (option) {
            case LOTTO4_STRAIGHT -> selection.equals(pick4);
            case LOTTO4_BOX -> selection.length() == 4 && sortedDigits(selection).equals(sortedDigits(pick4));
            case LOTTO4_FRONT_PAIR -> selection.length() == 4
                && selection.endsWith("**")
                && pick4.startsWith(selection.substring(0, 2));
            case LOTTO4_BACK_PAIR -> selection.length() == 4
                && selection.startsWith("**")
                && pick4.endsWith(selection.substring(2, 4));
            default -> false;
        };
    }

    private boolean lotto5(String selection, Short rawOption, TicketResultFacts facts) {
        var option = BetOption.from(BetType.LOTTO5_PATTERN, rawOption);
        var s = digitsOnly(selection);
        if (s.length() != 5 || facts.lot1_3d() == null || facts.lot2_2d() == null || facts.lot3_2d() == null) {
            return false;
        }

        return switch (option) {
            case LOTTO5_LOT1_LOT2 -> s.equals(facts.lot1_3d() + facts.lot2_2d());
            case LOTTO5_LOT1_LOT3 -> s.equals(facts.lot1_3d() + facts.lot3_2d());
            case LOTTO5_MIXED_1_2_3 -> s.equals(
                facts.lot1_3d().substring(2, 3) + facts.lot2_2d() + facts.lot3_2d());
            default -> false;
        };
    }

    private boolean appearsInOrder(List<String> values, String a, String b) {
        int aIndex = values.indexOf(a);
        if (aIndex < 0) {
            return false;
        }
        return values.subList(aIndex + 1, values.size()).contains(b);
    }

    private static String sortedDigits(String value) {
        var chars = value.toCharArray();
        java.util.Arrays.sort(chars);
        return new String(chars);
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

    private record TicketResultFacts(
        List<String> orderedTwoDigits,
        Set<String> twoDigits,
        String lot1_3d,
        String lot2_2d,
        String lot3_2d,
        String pick3,
        String pick4
    ) {
        static TicketResultFacts from(DrawResultProjection projection) {
            var ordered = new ArrayList<String>();
            add2d(ordered, projection.lot1());
            add2d(ordered, projection.lot2());
            add2d(ordered, projection.lot3());
            add2d(ordered, projection.lot4());
            if (projection.derivedPairs() != null) {
                projection.derivedPairs().forEach(v -> add2d(ordered, v));
            }
            return new TicketResultFacts(
                List.copyOf(ordered),
                Set.copyOf(new LinkedHashSet<>(ordered)),
                normalizeDigits(projection.lot1(), 3),
                normalizeDigits(projection.lot2(), 2),
                normalizeDigits(projection.lot3(), 2),
                firstNonNull(normalizeDigits(projection.lot1(), 3), normalizeDigits(projection.lot4(), 3)),
                firstNonNull(normalizeDigits(projection.lot4(), 4), normalizeDigits(projection.lot1(), 4))
            );
        }

        private static void add2d(List<String> out, String value) {
            var normalized = normalizeDigits(value, 2);
            if (normalized != null) {
                out.add(normalized);
            }
        }

        private static String firstNonNull(String first, String second) {
            return first != null ? first : second;
        }
    }
}
