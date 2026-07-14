package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionDecisionStatus;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Money path: promotional charge waiver. A WAIVE_CHARGE effect must waive ONLY the charge type it
 * targets (keeping the original amount for print/audit but making it non-buyer-facing, so the
 * ticket total drops), and must be a strict no-op for any other effect type. Currently only
 * exercised via a heavy Spring integration test.
 */
@DisplayName("PromotionChargeApplier")
class PromotionChargeApplierTest {

  private static final CurrencyCode HTG = CurrencyCode.of("HTG");
  private static final PromotionRuleId RULE_ID = PromotionRuleId.of(UUID.randomUUID());
  private static final PromotionDecisionId DECISION_ID = PromotionDecisionId.of(UUID.randomUUID());

  private final PromotionChargeApplier applier = new PromotionChargeApplier();

  private static TicketCharge buyerCharge(TicketChargeType type) {
    return new TicketCharge(type, new Money(new BigDecimal("5.00"), HTG), ChargePaidBy.BUYER);
  }

  private static PromotionEffect effect(PromotionEffectType type, String appliesTo, String reason) {
    return new PromotionEffect(
        RULE_ID, type, "BOLET", 1, null, "HTG", appliesTo, reason, PromotionChoiceMode.NONE);
  }

  private static PromotionDecision decision(PromotionEffect effect) {
    return new PromotionDecision(
        DECISION_ID,
        PromotionDecisionStatus.APPLIED,
        PromotionEvaluationPhase.SALE_CONFIRMATION,
        Instant.parse("2026-07-09T12:00:00Z"),
        "ctx-hash",
        "v1",
        List.of(effect),
        List.of());
  }

  @Nested
  @DisplayName("WAIVE_CHARGE")
  class WaiveCharge {

    @Test
    @DisplayName("waives only the targeted charge type, keeping amount but dropping buyer-facing")
    void waivesTargetedType() {
      var sms = buyerCharge(TicketChargeType.BUYER_SMS);
      var whatsapp = buyerCharge(TicketChargeType.BUYER_WHATSAPP);
      var charges = new ArrayList<>(List.of(sms, whatsapp));

      var effect = effect(PromotionEffectType.WAIVE_CHARGE, "BUYER_SMS", "welcome promo");
      applier.apply(charges, effect, decision(effect));

      var waived = charges.get(0);
      var untouched = charges.get(1);

      assertThat(waived.type()).isEqualTo(TicketChargeType.BUYER_SMS);
      assertThat(waived.isWaived()).isTrue();
      assertThat(waived.isBuyerFacing()).isFalse();
      assertThat(waived.amount()).isEqualTo(sms.amount()); // amount preserved for audit
      assertThat(waived.waivedByRuleId()).isEqualTo(RULE_ID);
      assertThat(waived.waivedByDecisionId()).isEqualTo(DECISION_ID);
      assertThat(waived.waivedEffectType()).isEqualTo("WAIVE_CHARGE");
      assertThat(waived.waivedLabel()).isEqualTo("welcome promo");

      assertThat(untouched.isWaived()).isFalse();
      assertThat(untouched.isBuyerFacing()).isTrue();
    }

    @Test
    @DisplayName("blank reason falls back to the effect type as the label")
    void labelFallsBackToEffectType() {
      var charges = new ArrayList<>(List.of(buyerCharge(TicketChargeType.BUYER_SMS)));

      var effect = effect(PromotionEffectType.WAIVE_CHARGE, "BUYER_SMS", "   ");
      applier.apply(charges, effect, decision(effect));

      assertThat(charges.get(0).waivedLabel()).isEqualTo("WAIVE_CHARGE");
    }

    @Test
    @DisplayName("no charge of the targeted type → nothing is waived")
    void noMatchingChargeType() {
      var charges = new ArrayList<>(List.of(buyerCharge(TicketChargeType.BUYER_WHATSAPP)));

      var effect = effect(PromotionEffectType.WAIVE_CHARGE, "BUYER_SMS", "promo");
      applier.apply(charges, effect, decision(effect));

      assertThat(charges.get(0).isWaived()).isFalse();
      assertThat(charges.get(0).isBuyerFacing()).isTrue();
    }
  }

  @Nested
  @DisplayName("non-WAIVE_CHARGE effects")
  class NonWaive {

    @Test
    @DisplayName("BOOST_ODDS effect is a strict no-op on charges")
    void boostOddsIsNoOp() {
      var charges = new ArrayList<>(List.of(buyerCharge(TicketChargeType.BUYER_SMS)));

      var effect = effect(PromotionEffectType.BOOST_ODDS, "BUYER_SMS", "promo");
      applier.apply(charges, effect, decision(effect));

      assertThat(charges.get(0).isWaived()).isFalse();
      assertThat(charges.get(0).isBuyerFacing()).isTrue();
    }
  }
}
