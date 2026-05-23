package com.tchalanet.server.core.promotion.internal.application.engine;

import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshot;
import com.tchalanet.server.core.promotion.api.model.FreeLineGrant;
import com.tchalanet.server.core.promotion.api.model.PayoutModifier;
import com.tchalanet.server.core.promotion.api.model.PrizeRank;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.PromotionNotice;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRuleDefinition;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SimplePromotionRuleEngine implements PromotionRuleEngine {

  @Override
  public PromotionDecision evaluate(List<PromotionRuleDefinition> rules, PromotionEvaluationContext ctx) {
    var freeLines = new ArrayList<FreeLineGrant>();
    var payoutModifiers = new ArrayList<PayoutModifier>();
    var notices = new ArrayList<PromotionNotice>();
    var snapshots = new ArrayList<AppliedPromotionSnapshot>();

    for (var rule : rules) {
      if (!matchesCommon(rule, ctx)) {
        continue;
      }
      var effectType = PromotionEffectType.valueOf(String.valueOf(rule.effectJson().get("effectType")));
      switch (effectType) {
        case FREE_GAME_LINE -> addFreeLine(rule, freeLines, notices);
        case PAYOUT_MULTIPLIER_OVERRIDE -> addPayoutOverride(rule, payoutModifiers, notices, snapshots, ctx);
        default -> {
          // V1 skeleton: add other effect types incrementally.
        }
      }
    }

    return new PromotionDecision(freeLines, payoutModifiers, List.of(), List.of(), notices, snapshots);
  }

  private boolean matchesCommon(PromotionRuleDefinition rule, PromotionEvaluationContext ctx) {
    if (!rule.active()) return false;
    if (ctx.offline() && !rule.offlineAllowed()) return false;
    if (rule.startsAt() != null && ctx.saleAt().isBefore(rule.startsAt())) return false;
    if (rule.endsAt() != null && !ctx.saleAt().isBefore(rule.endsAt())) return false;

    var conditions = rule.conditionJson();
    if (conditions.containsKey("cartPaidTotalGte")) {
      var min = new BigDecimal(String.valueOf(conditions.get("cartPaidTotalGte")));
      if (ctx.paidTotal().compareTo(min) < 0) return false;
    }
    if (conditions.containsKey("saleDate")) {
      var expected = LocalDate.parse(String.valueOf(conditions.get("saleDate")));
      var actual = ZonedDateTime.ofInstant(ctx.saleAt(), rule.timezone()).toLocalDate();
      if (!actual.equals(expected)) return false;
    }
    if (conditions.containsKey("saleTimeBefore")) {
      var before = LocalTime.parse(String.valueOf(conditions.get("saleTimeBefore")));
      var actual = ZonedDateTime.ofInstant(ctx.saleAt(), rule.timezone()).toLocalTime();
      if (!actual.isBefore(before)) return false;
    }
    return true;
  }

  private void addFreeLine(PromotionRuleDefinition rule, List<FreeLineGrant> out, List<PromotionNotice> notices) {
    var json = rule.effectJson();
    out.add(new FreeLineGrant(
        rule.code(),
        rule.ruleVersion(),
        String.valueOf(json.get("gameCode")),
        Integer.parseInt(String.valueOf(json.getOrDefault("quantity", "1"))),
        new BigDecimal(String.valueOf(json.get("effectiveStakeAmount"))),
        Boolean.parseBoolean(String.valueOf(json.getOrDefault("requiresUserSelection", "true")))
    ));
    notices.add(new PromotionNotice(rule.code(), rule.name(), "INFO", Map.of("ruleType", rule.ruleType())));
  }

  private void addPayoutOverride(
      PromotionRuleDefinition rule,
      List<PayoutModifier> modifiers,
      List<PromotionNotice> notices,
      List<AppliedPromotionSnapshot> snapshots,
      PromotionEvaluationContext ctx
  ) {
    var json = rule.effectJson();
    var applied = new BigDecimal(String.valueOf(json.get("appliedMultiplier")));
    var base = json.get("baseMultiplier") == null ? null : new BigDecimal(String.valueOf(json.get("baseMultiplier")));
    var gameCode = String.valueOf(json.get("gameCode"));
    var rank = PrizeRank.valueOf(String.valueOf(json.getOrDefault("prizeRank", "ANY")));

    modifiers.add(new PayoutModifier(
        rule.code(), rule.ruleVersion(), PromotionEffectType.PAYOUT_MULTIPLIER_OVERRIDE,
        gameCode, rank, base, applied, null
    ));
    snapshots.add(new AppliedPromotionSnapshot(
        rule.code(), rule.ruleVersion(), PromotionEffectType.PAYOUT_MULTIPLIER_OVERRIDE, null,
        Map.of("gameCode", gameCode, "prizeRank", rank.name(), "baseMultiplier", base, "appliedMultiplier", applied),
        ctx.saleAt()
    ));
    notices.add(new PromotionNotice(rule.code(), rule.name(), "INFO", Map.of("appliedMultiplier", applied)));
  }
}
