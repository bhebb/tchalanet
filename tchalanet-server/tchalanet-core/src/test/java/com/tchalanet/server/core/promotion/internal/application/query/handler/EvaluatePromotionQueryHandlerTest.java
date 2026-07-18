package com.tchalanet.server.core.promotion.internal.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.model.PromotionNoticeCodes;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.query.EvaluatePromotionQuery;
import com.tchalanet.server.core.promotion.internal.application.service.PromotionContextHasher;
import com.tchalanet.server.core.promotion.internal.application.service.PromotionRuleEvaluator;
import com.tchalanet.server.core.promotion.internal.application.service.PromotionTerminalOverrideMerger;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import com.tchalanet.server.core.promotion.internal.domain.model.SellerTerminalPromotionEffectOverride;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EvaluatePromotionQueryHandler")
class EvaluatePromotionQueryHandlerTest {

  @Test
  @DisplayName("applies seller terminal overrides before evaluating promotions")
  void appliesSellerTerminalOverrides() {
    var tenantId = TenantId.of(UUID.fromString("C1000000-0000-0000-0000-000000000001"));
    var terminalId = SellerTerminalId.of(UUID.fromString("D1000000-0000-0000-0000-000000000001"));
    var rule = maryajRule();
    var handler =
        new EvaluatePromotionQueryHandler(
            () -> List.of(rule),
            (tenant, terminal) ->
                List.of(
                    new SellerTerminalPromotionEffectOverride(
                        UUID.fromString("E1000000-0000-0000-0000-000000000001"),
                        terminalId,
                        rule.campaignId(),
                        rule.id(),
                        PromotionEffectType.FREE_GAME_LINE,
                        "HT_MARYAJ_GRATIS",
                        true,
                        2,
                        PromotionChoiceMode.SELLER_SELECTS,
                        null,
                        false,
                        0)),
            new PromotionRuleEvaluator(),
            new PromotionContextHasher(),
            new PromotionTerminalOverrideMerger(),
            () -> UUID.fromString("F1000000-0000-0000-0000-000000000001"),
            Clock.fixed(Instant.parse("2026-05-27T10:15:30Z"), ZoneOffset.UTC));

    var decision = handler.handle(new EvaluatePromotionQuery(context(tenantId, terminalId)));

    assertThat(decision.effects()).hasSize(1);
    assertThat(decision.effects().getFirst().quantity()).isEqualTo(2);
    assertThat(decision.effects().getFirst().choiceMode())
        .isEqualTo(PromotionChoiceMode.SELLER_SELECTS);
    assertThat(decision.contextHash()).contains(".");
    assertThat(decision.notices())
        .contains(
            PromotionNoticeCodes.DECISION_APPLIED,
            PromotionNoticeCodes.TERMINAL_OVERRIDE_APPLIED);
  }

  private PromotionEvaluationContext context(TenantId tenantId, SellerTerminalId terminalId) {
    return new PromotionEvaluationContext(
        tenantId,
        PromotionEvaluationPhase.SALE_CONFIRMATION,
        Instant.parse("2026-05-27T10:15:30Z"),
        null,
        List.of(),
        null,
        List.of(),
        terminalId,
        null,
        new BigDecimal("500"),
        "HTG",
        List.of("HT_BOLET"),
        false);
  }

  private PromotionRule maryajRule() {
    var campaignId =
        PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000001"));
    var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000001"));
    return new PromotionRule(
        ruleId,
        campaignId,
        "maryaj-gratis",
        100,
        null,
        null,
        List.of(),
        List.of(
            new PromotionEffect(
                ruleId,
                campaignId,
                "maryaj-gratis",
                PromotionEffectType.FREE_GAME_LINE,
                "HT_MARYAJ_GRATIS",
                1,
                BigDecimal.ONE,
                "HTG",
                null,
                null,
                PromotionChoiceMode.AUTO_GENERATE)));
  }
}
