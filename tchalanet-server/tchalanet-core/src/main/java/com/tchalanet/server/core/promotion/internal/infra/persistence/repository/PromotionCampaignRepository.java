package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaignJpaEntity, UUID> {
    default PromotionCampaignJpaEntity getRequired(UUID value) {
        return this.findById(value).orElseThrow(() -> new TchNotFoundException(value.toString(), "PromotionCampaign not found."));
    }
}

