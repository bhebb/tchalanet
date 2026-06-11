package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.core.sales.internal.infra.persistence.entity.preparation.SalePreparationPromotionLineJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalePreparationPromotionLineRepository
    extends JpaRepository<SalePreparationPromotionLineJpaEntity, UUID> {

    List<SalePreparationPromotionLineJpaEntity> findByPreparationIdOrderByLineRefAsc(UUID preparationId);

    Optional<SalePreparationPromotionLineJpaEntity> findByPreparationIdAndLineRef(
        UUID preparationId, String lineRef);
}
