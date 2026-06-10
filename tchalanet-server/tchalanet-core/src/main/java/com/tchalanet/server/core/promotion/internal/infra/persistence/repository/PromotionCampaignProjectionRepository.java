package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface PromotionCampaignProjectionRepository extends JpaRepository<PromotionCampaignJpaEntity, UUID> {

    @Query("""
        SELECT new com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignProjection(
            p.id, p.code, p.name, p.status, p.priority, p.startsAt, p.endsAt, p.createdAt
        )
        FROM PromotionCampaignJpaEntity p
        WHERE p.deletedAt IS NULL
    """)
    Page<PromotionCampaignProjection> findSummaries(Pageable pageable);

    java.util.Optional<PromotionCampaignJpaEntity> findByCodeAndDeletedAtIsNull(String code);

    @Query("""
        SELECT new com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignProjection(
            p.id, p.code, p.name, p.status, p.priority, p.startsAt, p.endsAt, p.createdAt
        )
        FROM PromotionCampaignJpaEntity p
        WHERE p.id = :id
    """)
    PromotionCampaignProjection findProjectionById(UUID id);
}

