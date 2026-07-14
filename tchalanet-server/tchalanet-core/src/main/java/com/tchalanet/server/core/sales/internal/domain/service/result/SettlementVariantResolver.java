package com.tchalanet.server.core.sales.internal.domain.service.result;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.sales.api.model.coverage.SettlementPayoutMode;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.WinMode;
import java.util.List;
import java.util.stream.Collectors;

public final class SettlementVariantResolver {

  private SettlementVariantResolver() {}

  public static PricingVariantCode resolve(BetType betType, Short rawOption, String selection) {
    var coverage = resolveCoverage(betType, rawOption, selection);
    if (coverage.variants().size() != 1) {
      throw new IllegalArgumentException(
          "Bet option resolves to multiple coverages: " + coverage.option());
    }
    return coverage.variants().getFirst().pricingVariantCode();
  }

  public static CoverageResolution resolveCoverage(
      BetType betType, Short rawOption, String selection) {
    if (betType == null) {
      throw new IllegalArgumentException("betType is required");
    }

    var option =
        BetOption.allowedFor(betType).isEmpty() ? null : BetOption.from(betType, rawOption);
    if (option == BetOption.LOTTO3_EXACT_PLUS_BOX) {
      return new CoverageResolution(
          option,
          SettlementPayoutMode.RANGE_ALTERNATIVE,
          List.of(
              new CoverageVariant(PricingVariantCode.LOTTO3_STRAIGHT, WinMode.ALTERNATIVE),
              new CoverageVariant(resolveLotto3Box(selection), WinMode.ALTERNATIVE)));
    }
    if (option == BetOption.LOTTO4_EXACT_PLUS_BOX) {
      return new CoverageResolution(
          option,
          SettlementPayoutMode.RANGE_ALTERNATIVE,
          List.of(
              new CoverageVariant(PricingVariantCode.LOTTO4_STRAIGHT, WinMode.ALTERNATIVE),
              new CoverageVariant(resolveLotto4Box(selection), WinMode.ALTERNATIVE)));
    }

    var variant =
        switch (betType) {
          case MATCH_1_2D -> PricingVariantCode.MATCH_1_2D;
          case MATCH_2_2D -> PricingVariantCode.MATCH_2_2D;
          case MATCH_3_2D -> PricingVariantCode.MATCH_3_2D;

          case MARRIAGE_2D2D -> resolveMarriage(rawOption);
          case LOTTO3_3D -> resolveLotto3(rawOption, selection);
          case LOTTO4_PATTERN -> resolveLotto4(rawOption, selection);
          case LOTTO5_PATTERN -> resolveLotto5(rawOption);
        };

    return new CoverageResolution(
        option,
        SettlementPayoutMode.SINGLE,
        List.of(new CoverageVariant(variant, WinMode.ALTERNATIVE)));
  }

  private static PricingVariantCode resolveMarriage(Short rawOption) {
    var option = BetOption.from(BetType.MARRIAGE_2D2D, rawOption);

    return switch (option) {
      case MARRIAGE_EXACT_ORDER -> PricingVariantCode.MARRIAGE_EXACT_ORDER;
      case MARRIAGE_REVERSE_ALLOWED -> PricingVariantCode.MARRIAGE_REVERSE_ALLOWED;
      default -> throw unsupported(option, BetType.MARRIAGE_2D2D);
    };
  }

  private static PricingVariantCode resolveLotto3(Short rawOption, String selection) {
    var option = BetOption.from(BetType.LOTTO3_3D, rawOption);

    return switch (option) {
      case LOTTO3_STRAIGHT -> PricingVariantCode.LOTTO3_STRAIGHT;
      case LOTTO3_BOX -> resolveLotto3Box(selection);
      default -> throw unsupported(option, BetType.LOTTO3_3D);
    };
  }

  private static PricingVariantCode resolveLotto3Box(String selection) {
    var digits = requireDigits(selection, 3, "Loto 3 box");

    return switch (distinctDigitCount(digits)) {
      case 2 -> PricingVariantCode.LOTTO3_BOX_3_WAY;
      case 3 -> PricingVariantCode.LOTTO3_BOX_6_WAY;
      default -> throw new IllegalArgumentException("Invalid Loto 3 box pattern: " + selection);
    };
  }

  private static PricingVariantCode resolveLotto4(Short rawOption, String selection) {
    var option = BetOption.from(BetType.LOTTO4_PATTERN, rawOption);

    return switch (option) {
      case LOTTO4_STRAIGHT -> PricingVariantCode.LOTTO4_STRAIGHT;
      case LOTTO4_FRONT_PAIR -> PricingVariantCode.LOTTO4_FRONT_PAIR;
      case LOTTO4_BACK_PAIR -> PricingVariantCode.LOTTO4_BACK_PAIR;
      case LOTTO4_BOX -> resolveLotto4Box(selection);
      default -> throw unsupported(option, BetType.LOTTO4_PATTERN);
    };
  }

  private static PricingVariantCode resolveLotto4Box(String selection) {
    var digits = requireDigits(selection, 4, "Loto 4 box");
    var counts = digitCounts(digits);

    if (counts.contains(4)) {
      throw new IllegalArgumentException("Invalid Loto 4 box pattern: all digits are identical");
    }

    if (counts.contains(3)) {
      return PricingVariantCode.LOTTO4_BOX_4_WAY;
    }

    if (counts.size() == 2 && counts.stream().allMatch(count -> count == 2)) {
      return PricingVariantCode.LOTTO4_BOX_6_WAY;
    }

    if (counts.contains(2) && counts.size() == 3) {
      return PricingVariantCode.LOTTO4_BOX_12_WAY;
    }

    if (counts.size() == 4) {
      return PricingVariantCode.LOTTO4_BOX_24_WAY;
    }

    throw new IllegalArgumentException("Unsupported Loto 4 box pattern: " + selection);
  }

  private static PricingVariantCode resolveLotto5(Short rawOption) {
    var option = BetOption.from(BetType.LOTTO5_PATTERN, rawOption);

    return switch (option) {
      case LOTTO5_LOT1_LOT2 -> PricingVariantCode.LOTTO5_LOT1_LOT2;
      case LOTTO5_LOT1_LOT3 -> PricingVariantCode.LOTTO5_LOT1_LOT3;
      case LOTTO5_MIXED_1_2_3 -> PricingVariantCode.LOTTO5_MIXED_1_2_3;
      default -> throw unsupported(option, BetType.LOTTO5_PATTERN);
    };
  }

  private static String requireDigits(String value, int expectedLen, String label) {
    var digits = digitsOnly(value);
    if (digits.length() != expectedLen) {
      throw new IllegalArgumentException(label + " requires " + expectedLen + " digits: " + value);
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
    return value
        .chars()
        .boxed()
        .collect(Collectors.groupingBy(digit -> digit, Collectors.counting()))
        .values()
        .stream()
        .map(Long::intValue)
        .sorted()
        .toList();
  }

  private static IllegalArgumentException unsupported(BetOption option, BetType expectedBetType) {
    return new IllegalArgumentException(
        "Unsupported betOption " + option + " for betType " + expectedBetType);
  }
}
