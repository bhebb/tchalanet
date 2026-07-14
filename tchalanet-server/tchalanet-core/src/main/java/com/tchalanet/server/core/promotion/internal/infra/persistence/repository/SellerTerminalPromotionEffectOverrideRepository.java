package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.SellerTerminalPromotionEffectOverrideJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerTerminalPromotionEffectOverrideRepository
    extends JpaRepository<SellerTerminalPromotionEffectOverrideJpaEntity, UUID> {

  List<SellerTerminalPromotionEffectOverrideJpaEntity>
      findByTenantIdAndSellerTerminalIdAndActiveTrueOrderByCampaignIdAscRuleIdAscEffectTypeAscGameCodeAsc(
          UUID tenantId, UUID sellerTerminalId);
}
