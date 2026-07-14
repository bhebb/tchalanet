package com.tchalanet.server.core.promotion.internal.domain.model;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import java.util.UUID;

public record SellerTerminalPromotionEffectOverride(
    UUID id,
    SellerTerminalId sellerTerminalId,
    PromotionCampaignId campaignId,
    PromotionRuleId ruleId,
    PromotionEffectType effectType,
    String gameCode,
    Boolean effectEnabled,
    Integer quantity,
    PromotionChoiceMode choiceMode,
    SelectionGenerationStrategy generationStrategy,
    Boolean regenerableBeforeConfirm,
    Integer maxRegenerationsBeforeConfirm) {
  public boolean matches(PromotionRule rule, PromotionEffectType type, String effectGameCode) {
    return campaignId.equals(rule.campaignId())
        && ruleId.equals(rule.id())
        && effectType == type
        && matchesGameCode(effectGameCode);
  }

  public String signature() {
    return id
        + ":"
        + sellerTerminalId.value()
        + ":"
        + campaignId.value()
        + ":"
        + ruleId.value()
        + ":"
        + effectType
        + ":"
        + normalizedGameCode(gameCode)
        + ":"
        + effectEnabled
        + ":"
        + quantity
        + ":"
        + choiceMode
        + ":"
        + generationStrategy
        + ":"
        + regenerableBeforeConfirm
        + ":"
        + maxRegenerationsBeforeConfirm;
  }

  private boolean matchesGameCode(String effectGameCode) {
    var normalized = normalizedGameCode(gameCode);
    return "*".equals(normalized) || normalized.equals(normalizedGameCode(effectGameCode));
  }

  private String normalizedGameCode(String value) {
    return value == null || value.isBlank() ? "*" : value.trim().toUpperCase();
  }
}
