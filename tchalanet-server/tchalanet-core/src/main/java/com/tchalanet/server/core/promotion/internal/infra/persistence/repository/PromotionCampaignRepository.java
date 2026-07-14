package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionCampaignRepository
    extends JpaRepository<PromotionCampaignJpaEntity, UUID> {
  default PromotionCampaignJpaEntity getRequired(UUID value) {
    return this.findById(value)
        .orElseThrow(
            () -> new TchNotFoundException(value.toString(), "PromotionCampaign not found."));
  }
}
