package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityMode;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleEffectJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.mapper.PromotionCampaignJpaMapper;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionCampaignRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleEffectRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleEligibilityLineRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PromotionRuleWriteSupportTest {

  private static final TenantId TENANT =
      TenantId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  private static final PromotionCampaignId CAMPAIGN_ID =
      PromotionCampaignId.of(UUID.fromString("22222222-2222-2222-2222-222222222222"));
  private static final PromotionRuleId RULE_ID =
      PromotionRuleId.of(UUID.fromString("33333333-3333-3333-3333-333333333333"));

  @Test
  void updateRuleEffectsPersistsAndReadsCustomMaryajGratisQuantityTiers() {
    var campaigns = mock(PromotionCampaignRepository.class);
    var rules = mock(PromotionRuleRepository.class);
    var effects = mock(PromotionRuleEffectRepository.class);
    var eligibility = mock(PromotionRuleEligibilityLineRepository.class);
    var viewAssembler = mock(PromotionCampaignViewAssembler.class);
    var support =
        new PromotionRuleWriteSupport(campaigns, rules, effects, eligibility, viewAssembler);

    when(campaigns.getRequired(CAMPAIGN_ID.value())).thenReturn(defaultMaryajCampaign());
    when(rules.findByIdAndCampaignId(RULE_ID.value(), CAMPAIGN_ID.value()))
        .thenReturn(Optional.of(rule()));
    when(effects.save(any(PromotionRuleEffectJpaEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    support.updateRuleEffects(
        new UpdatePromotionRuleEffectsCommand(
            TENANT,
            CAMPAIGN_ID,
            RULE_ID,
            List.of(
                new PromotionEffectConfigInput(
                    PromotionEffectType.FREE_GAME_LINE,
                    Map.of(
                        "gameCode", "HT_MARYAJ_GRATIS",
                        "payoutBaseAmount", "75",
                        "quantity", "1",
                        "quantityMode", PromotionQuantityMode.TIERED_PAID_AMOUNT.name(),
                        "maxQuantity", "4",
                        "quantityTiers",
                            List.of(
                                Map.of(
                                    "minPaidAmount",
                                    "500",
                                    "maxPaidAmount",
                                    "999",
                                    "quantity",
                                    "2"),
                                Map.of("minPaidAmount", "1000", "quantity", "4")),
                        "choiceMode", PromotionChoiceMode.AUTO_GENERATE.name(),
                        "generationStrategy", "RANDOM",
                        "regenerableBeforeConfirm", "true",
                        "maxRegenerationsBeforeConfirm", "5")))));

    var saved = ArgumentCaptor.forClass(PromotionRuleEffectJpaEntity.class);
    verify(effects).deleteByRuleId(RULE_ID.value());
    verify(effects).flush();
    verify(effects).save(saved.capture());

    assertThat(saved.getValue().getQuantityTiers())
        .containsExactly(
            Map.of("minPaidAmount", "500", "maxPaidAmount", "999", "quantity", 2),
            Map.of("minPaidAmount", "1000", "quantity", 4));

    PromotionCampaignJpaMapper mapper = new PromotionCampaignJpaMapper() {};
    var view = mapper.toEffectView(saved.getValue());

    assertThat(view.params().get("quantityTiers"))
        .isEqualTo(
            List.of(
                Map.of("minPaidAmount", "500", "maxPaidAmount", "999", "quantity", 2),
                Map.of("minPaidAmount", "1000", "quantity", 4)));
    assertThat(view.params())
        .containsEntry("quantityMode", "TIERED_PAID_AMOUNT")
        .containsEntry("maxQuantity", 4)
        .containsEntry("payoutBaseAmount", new BigDecimal("75"));
  }

  @Test
  void boostOddsWithMoreThanFourDecimalsIsRejectedAtConfigTime() {
    var campaigns = mock(PromotionCampaignRepository.class);
    var rules = mock(PromotionRuleRepository.class);
    var effects = mock(PromotionRuleEffectRepository.class);
    var eligibility = mock(PromotionRuleEligibilityLineRepository.class);
    var viewAssembler = mock(PromotionCampaignViewAssembler.class);
    var support =
        new PromotionRuleWriteSupport(campaigns, rules, effects, eligibility, viewAssembler);

    when(campaigns.getRequired(CAMPAIGN_ID.value())).thenReturn(defaultMaryajCampaign());
    when(rules.findByIdAndCampaignId(RULE_ID.value(), CAMPAIGN_ID.value()))
        .thenReturn(Optional.of(rule()));

    // A boost with 5 significant decimals would crash the sale at
    // PromotionOddsBoostApplier (setScale(4, UNNECESSARY)); it must be rejected here.
    var command =
        new UpdatePromotionRuleEffectsCommand(
            TENANT,
            CAMPAIGN_ID,
            RULE_ID,
            List.of(
                new PromotionEffectConfigInput(
                    PromotionEffectType.BOOST_ODDS,
                    Map.of(
                        "gameCode", "HT_BOLET",
                        "oddsOverride", "2.12345"))));

    assertThatThrownBy(() -> support.updateRuleEffects(command))
        .isInstanceOf(ProblemRestException.class)
        .hasMessageContaining("promotion.rule.boost_odds_scale");
  }

  private PromotionCampaignJpaEntity defaultMaryajCampaign() {
    var campaign = new PromotionCampaignJpaEntity();
    campaign.setId(CAMPAIGN_ID.value());
    campaign.setTenantId(TENANT.value());
    campaign.setCode("DEFAULT_MARYAJ_GRATIS");
    campaign.setName("Maryaj gratis");
    campaign.setStatus(PromotionCampaignStatus.ACTIVE);
    campaign.setPriority(10);
    return campaign;
  }

  private PromotionRuleJpaEntity rule() {
    var rule = new PromotionRuleJpaEntity();
    rule.setId(RULE_ID.value());
    rule.setTenantId(TENANT.value());
    rule.setCampaignId(CAMPAIGN_ID.value());
    rule.setRuleKey("maryaj-gratis-default");
    rule.setPriority(10);
    return rule;
  }
}
