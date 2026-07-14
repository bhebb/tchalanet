package com.tchalanet.server.core.sales.internal.application.service.result;

import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultProjection;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.sales.api.model.line.TicketLineResult;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementWinMode;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TicketWinningCalculator {

  private static final Pattern SEP = Pattern.compile("[,\\s\\-]+");

  public HashMap<TicketLineId, TicketLineResult> computeLineResults(
      Ticket ticket, DrawResultProjection projection) {
    var facts = TicketResultFacts.from(projection);

    var results = new HashMap<TicketLineId, TicketLineResult>();
    var zero = Money.zero(ticket.money().currency());

    for (TicketLine line : ticket.lines()) {
      var evaluation = evaluateLine(line, facts);

      results.put(
          line.id(),
          new TicketLineResult(
              evaluation.won() ? TicketLineResultStatus.WON : TicketLineResultStatus.LOST,
              evaluation.won() ? evaluation.payoutAmount() : zero));
    }

    return results;
  }

  private WinningEvaluation evaluateLine(TicketLine line, TicketResultFacts facts) {
    var selection = safe(line.selection().key().value());
    var payout = Money.zero(line.stakeAmount().currency());
    var won = false;

    var snapshot = line.settlementTermsSnapshot();
    if (snapshot == null || snapshot.terms().isEmpty()) {
      throw new IllegalStateException("settlement.terms_snapshot_required");
    }

    for (var term : snapshot.terms()) {
      if (!wins(selection, facts, term.ruleCode().toPricingVariantCode())) {
        continue;
      }

      won = true;
      payout = combinePayout(payout, realizedPayout(line, term), term);
    }

    return new WinningEvaluation(won, payout);
  }

  private boolean wins(String selection, TicketResultFacts facts, PricingVariantCode variant) {
    return switch (variant) {
      case MATCH_1_2D -> match2d(selection, facts.lot1_2d());
      case MATCH_2_2D -> match2d(selection, facts.lot2_2d());
      case MATCH_3_2D -> match2d(selection, facts.lot3_2d());

      case MARRIAGE_EXACT_ORDER -> marriageExact(selection, facts.orderedTwoDigits());
      case MARRIAGE_REVERSE_ALLOWED -> marriageReverseAllowed(selection, facts.orderedTwoDigits());

      case LOTTO3_STRAIGHT -> exactDigits(selection, facts.pick3(), 3);
      case LOTTO3_BOX_3_WAY, LOTTO3_BOX_6_WAY -> boxDigits(selection, facts.pick3(), 3);

      case LOTTO4_STRAIGHT -> exactDigits(selection, facts.pick4(), 4);
      case LOTTO4_BOX_4_WAY, LOTTO4_BOX_6_WAY, LOTTO4_BOX_12_WAY, LOTTO4_BOX_24_WAY ->
          boxDigits(selection, facts.pick4(), 4);

      case LOTTO4_FRONT_PAIR -> frontPair(selection, facts.pick4());
      case LOTTO4_BACK_PAIR -> backPair(selection, facts.pick4());

      case LOTTO5_LOT1_LOT2 -> lotto5Lot1Lot2(selection, facts);
      case LOTTO5_LOT1_LOT3 -> lotto5Lot1Lot3(selection, facts);
      case LOTTO5_MIXED_1_2_3 -> lotto5Mixed123(selection, facts);
    };
  }

  private Money realizedPayout(TicketLine line, SettlementTermSnapshot winningTerm) {
    var rule = winningTerm.payoutRule();
    if (rule.type() == PayoutRuleType.FIXED_AMOUNT) {
      return new Money(
          rule.fixedAmount().setScale(2, RoundingMode.HALF_UP), line.stakeAmount().currency());
    }
    return new Money(
        winningTerm
            .payoutBaseAmount()
            .multiply(rule.multiplier())
            .setScale(2, RoundingMode.HALF_UP),
        line.stakeAmount().currency());
  }

  private Money combinePayout(
      Money current, Money winningPayout, SettlementTermSnapshot winningTerm) {
    if (winningTerm.winMode() == SettlementWinMode.CUMULATIVE) {
      return current.plus(winningPayout);
    }
    if (winningPayout.amount().compareTo(current.amount()) > 0) {
      return winningPayout;
    }
    return current;
  }

  private boolean match2d(String selection, String drawn2d) {
    var s = normalizeDigits(selection, 2);
    return s != null && drawn2d != null && s.equals(drawn2d);
  }

  private boolean marriageExact(String selection, List<String> orderedTwoDigits) {
    var parts = split(selection);
    if (parts.size() != 2) {
      return false;
    }

    var a = normalizeDigits(parts.get(0), 2);
    var b = normalizeDigits(parts.get(1), 2);

    return a != null && b != null && appearsInOrder(orderedTwoDigits, a, b);
  }

  private boolean marriageReverseAllowed(String selection, List<String> orderedTwoDigits) {
    var parts = split(selection);
    if (parts.size() != 2) {
      return false;
    }

    var a = normalizeDigits(parts.get(0), 2);
    var b = normalizeDigits(parts.get(1), 2);

    if (a == null || b == null) {
      return false;
    }

    if (a.equals(b)) {
      return frequency(orderedTwoDigits, a) >= 2;
    }

    return orderedTwoDigits.contains(a) && orderedTwoDigits.contains(b);
  }

  private boolean exactDigits(String selection, String drawn, int width) {
    var s = normalizeDigits(selection, width);
    return s != null && drawn != null && s.equals(drawn);
  }

  private boolean boxDigits(String selection, String drawn, int width) {
    var s = normalizeDigits(selection, width);
    return s != null && drawn != null && sortedDigits(s).equals(sortedDigits(drawn));
  }

  private boolean frontPair(String selection, String pick4) {
    var pair = normalizePairSelection(selection);
    return pair != null && pick4 != null && pick4.startsWith(pair);
  }

  private boolean backPair(String selection, String pick4) {
    var pair = normalizePairSelection(selection);
    return pair != null && pick4 != null && pick4.endsWith(pair);
  }

  private boolean lotto5Lot1Lot2(String selection, TicketResultFacts facts) {
    var s = normalizeDigits(selection, 5);

    return s != null
        && facts.lot1_3d() != null
        && facts.lot2_2d() != null
        && s.equals(facts.lot1_3d() + facts.lot2_2d());
  }

  private boolean lotto5Lot1Lot3(String selection, TicketResultFacts facts) {
    var s = normalizeDigits(selection, 5);

    return s != null
        && facts.lot1_3d() != null
        && facts.lot3_2d() != null
        && s.equals(facts.lot1_3d() + facts.lot3_2d());
  }

  private boolean lotto5Mixed123(String selection, TicketResultFacts facts) {
    var s = normalizeDigits(selection, 5);

    return s != null
        && facts.lot1_3d() != null
        && facts.lot2_2d() != null
        && facts.lot3_2d() != null
        && s.equals(facts.lot1_3d().substring(2, 3) + facts.lot2_2d() + facts.lot3_2d());
  }

  private boolean appearsInOrder(List<String> values, String a, String b) {
    int aIndex = values.indexOf(a);
    if (aIndex < 0) {
      return false;
    }

    return values.subList(aIndex + 1, values.size()).contains(b);
  }

  private int frequency(List<String> values, String target) {
    int count = 0;

    for (String value : values) {
      if (target.equals(value)) {
        count++;
      }
    }

    return count;
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

  private static String normalizePairSelection(String selection) {
    if (selection == null || selection.isBlank()) {
      return null;
    }

    var trimmed = selection.trim();

    if (trimmed.matches("\\d{2}")) {
      return trimmed;
    }

    if (trimmed.matches("\\d{2}\\*\\*")) {
      return trimmed.substring(0, 2);
    }

    if (trimmed.matches("\\*\\*\\d{2}")) {
      return trimmed.substring(2, 4);
    }

    var digits = digitsOnly(trimmed);
    return digits.length() == 2 ? digits : null;
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

  private static String last2Digits(String value) {
    var digits = digitsOnly(value);
    if (digits.length() < 2) {
      return null;
    }

    return digits.substring(digits.length() - 2);
  }

  private static String sortedDigits(String value) {
    var chars = value.toCharArray();
    java.util.Arrays.sort(chars);
    return new String(chars);
  }

  private static String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private record WinningEvaluation(boolean won, Money payoutAmount) {}

  private record TicketResultFacts(
      List<String> orderedTwoDigits,
      String lot1_3d,
      String lot1_2d,
      String lot2_2d,
      String lot3_2d,
      String lot4_2d,
      String pick3,
      String pick4) {

    static TicketResultFacts from(DrawResultProjection projection) {
      var lot1_3d = normalizeDigits(projection.lot1(), 3);
      var lot1_2d = last2Digits(projection.lot1());
      var lot2_2d = last2Digits(projection.lot2());
      var lot3_2d = last2Digits(projection.lot3());
      var lot4_2d = last2Digits(projection.lot4());

      var ordered = new ArrayList<String>();
      add2d(ordered, lot1_2d);
      add2d(ordered, lot2_2d);
      add2d(ordered, lot3_2d);
      add2d(ordered, lot4_2d);

      if (projection.derivedPairs() != null) {
        projection.derivedPairs().forEach(value -> add2d(ordered, last2Digits(value)));
      }

      return new TicketResultFacts(
          List.copyOf(ordered),
          lot1_3d,
          lot1_2d,
          lot2_2d,
          lot3_2d,
          lot4_2d,
          firstNonNull(
              normalizeDigits(projection.lot1(), 3), normalizeDigits(projection.lot4(), 3)),
          firstNonNull(
              normalizeDigits(projection.lot4(), 4), normalizeDigits(projection.lot1(), 4)));
    }

    private static void add2d(List<String> out, String value) {
      if (value != null && value.length() == 2) {
        out.add(value);
      }
    }

    private static String firstNonNull(String first, String second) {
      return first != null ? first : second;
    }
  }
}
