package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.internal.application.port.out.rule.SellerTerminalPromotionEffectOverrideReadPort;
import com.tchalanet.server.core.promotion.internal.domain.model.SellerTerminalPromotionEffectOverride;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.SellerTerminalPromotionEffectOverrideJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.SellerTerminalPromotionEffectOverrideRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SellerTerminalPromotionEffectOverrideJpaAdapter
    implements SellerTerminalPromotionEffectOverrideReadPort {
  private final SellerTerminalPromotionEffectOverrideRepository repository;

  @Override
  public List<SellerTerminalPromotionEffectOverride> findActiveOverrides(
      TenantId tenantId, SellerTerminalId sellerTerminalId) {
    return repository
        .findByTenantIdAndSellerTerminalIdAndActiveTrueOrderByCampaignIdAscRuleIdAscEffectTypeAscGameCodeAsc(
            tenantId.value(), sellerTerminalId.value())
        .stream()
        .map(this::toDomain)
        .toList();
  }

  private SellerTerminalPromotionEffectOverride toDomain(
      SellerTerminalPromotionEffectOverrideJpaEntity entity) {
    return new SellerTerminalPromotionEffectOverride(
        entity.getId(),
        SellerTerminalId.of(entity.getSellerTerminalId()),
        PromotionCampaignId.of(entity.getCampaignId()),
        PromotionRuleId.of(entity.getRuleId()),
        entity.getEffectType(),
        entity.getGameCode(),
        entity.getEffectEnabled(),
        entity.getQuantity(),
        entity.getChoiceMode(),
        entity.getGenerationStrategy(),
        entity.getRegenerableBeforeConfirm(),
        entity.getMaxRegenerationsBeforeConfirm());
  }
}
