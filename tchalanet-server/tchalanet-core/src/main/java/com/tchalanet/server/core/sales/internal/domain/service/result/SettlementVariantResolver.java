package com.tchalanet.server.core.sales.internal.domain.service.result;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.sales.internal.domain.model.result.SettlementVariant;
import java.util.List;
import java.util.stream.Collectors;

public final class SettlementVariantResolver {

    private SettlementVariantResolver() {}

    public static SettlementVariant resolve(
        BetType betType,
        Short rawOption,
        String selection
    ) {
        if (betType == null) {
            throw new IllegalArgumentException("betType is required");
        }

        return switch (betType) {
            case MATCH_1_2D -> SettlementVariant.MATCH_1_2D;
            case MATCH_2_2D -> SettlementVariant.MATCH_2_2D;
            case MATCH_3_2D -> SettlementVariant.MATCH_3_2D;

            case MARRIAGE_2D2D -> resolveMarriage(rawOption);
            case LOTTO3_3D -> resolveLotto3(rawOption, selection);
            case LOTTO4_PATTERN -> resolveLotto4(rawOption, selection);
            case LOTTO5_PATTERN -> resolveLotto5(rawOption);
        };
    }

    private static SettlementVariant resolveMarriage(Short rawOption) {
        var option = BetOption.from(BetType.MARRIAGE_2D2D, rawOption);

        return switch (option) {
            case MARRIAGE_EXACT_ORDER -> SettlementVariant.MARRIAGE_EXACT_ORDER;
            case MARRIAGE_REVERSE_ALLOWED -> SettlementVariant.MARRIAGE_REVERSE_ALLOWED;
            default -> throw unsupported(option, BetType.MARRIAGE_2D2D);
        };
    }

    private static SettlementVariant resolveLotto3(Short rawOption, String selection) {
        var option = BetOption.from(BetType.LOTTO3_3D, rawOption);

        return switch (option) {
            case LOTTO3_STRAIGHT -> SettlementVariant.LOTTO3_STRAIGHT;
            case LOTTO3_BOX -> {
                var digits = requireDigits(selection, 3, "Loto 3 box");

                yield switch (distinctDigitCount(digits)) {
                    case 2 -> SettlementVariant.LOTTO3_BOX_3_WAY;
                    case 3 -> SettlementVariant.LOTTO3_BOX_6_WAY;
                    default -> throw new IllegalArgumentException(
                        "Invalid Loto 3 box pattern: " + selection
                    );
                };
            }
            default -> throw unsupported(option, BetType.LOTTO3_3D);
        };
    }

    private static SettlementVariant resolveLotto4(Short rawOption, String selection) {
        var option = BetOption.from(BetType.LOTTO4_PATTERN, rawOption);

        return switch (option) {
            case LOTTO4_STRAIGHT -> SettlementVariant.LOTTO4_STRAIGHT;
            case LOTTO4_FRONT_PAIR -> SettlementVariant.LOTTO4_FRONT_PAIR;
            case LOTTO4_BACK_PAIR -> SettlementVariant.LOTTO4_BACK_PAIR;
            case LOTTO4_BOX -> resolveLotto4Box(selection);
            default -> throw unsupported(option, BetType.LOTTO4_PATTERN);
        };
    }

    private static SettlementVariant resolveLotto4Box(String selection) {
        var digits = requireDigits(selection, 4, "Loto 4 box");
        var counts = digitCounts(digits);

        if (counts.contains(4)) {
            throw new IllegalArgumentException(
                "Invalid Loto 4 box pattern: all digits are identical"
            );
        }

        if (counts.contains(3)) {
            return SettlementVariant.LOTTO4_BOX_4_WAY;
        }

        if (counts.size() == 2 && counts.stream().allMatch(count -> count == 2)) {
            return SettlementVariant.LOTTO4_BOX_6_WAY;
        }

        if (counts.contains(2) && counts.size() == 3) {
            return SettlementVariant.LOTTO4_BOX_12_WAY;
        }

        if (counts.size() == 4) {
            return SettlementVariant.LOTTO4_BOX_24_WAY;
        }

        throw new IllegalArgumentException("Unsupported Loto 4 box pattern: " + selection);
    }

    private static SettlementVariant resolveLotto5(Short rawOption) {
        var option = BetOption.from(BetType.LOTTO5_PATTERN, rawOption);

        return switch (option) {
            case LOTTO5_LOT1_LOT2 -> SettlementVariant.LOTTO5_LOT1_LOT2;
            case LOTTO5_LOT1_LOT3 -> SettlementVariant.LOTTO5_LOT1_LOT3;
            case LOTTO5_MIXED_1_2_3 -> SettlementVariant.LOTTO5_MIXED_1_2_3;
            default -> throw unsupported(option, BetType.LOTTO5_PATTERN);
        };
    }

    private static String requireDigits(String value, int expectedLen, String label) {
        var digits = digitsOnly(value);
        if (digits.length() != expectedLen) {
            throw new IllegalArgumentException(
                label + " requires " + expectedLen + " digits: " + value
            );
        }
        return digits;
    }

    private static String digitsOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\D", "");
    }

    private static int distinctDigitCount(String value) {
        return (int) value.chars().distinct().count();
    }

    private static List<Integer> digitCounts(String value) {
        return value.chars()
            .boxed()
            .collect(Collectors.groupingBy(
                digit -> digit,
                Collectors.counting()
            ))
            .values()
            .stream()
            .map(Long::intValue)
            .sorted()
            .toList();
    }

    private static IllegalArgumentException unsupported(
        BetOption option,
        BetType expectedBetType
    ) {
        return new IllegalArgumentException(
            "Unsupported betOption " + option + " for betType " + expectedBetType
        );
    }
}
