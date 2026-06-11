package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.preparation.SalePreparationJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalePreparationRepository extends JpaRepository<SalePreparationJpaEntity, UUID> {

    Optional<SalePreparationJpaEntity> findByIdempotencyKey(String idempotencyKey);

    List<SalePreparationJpaEntity> findTop100ByStatusAndExpiresAtBefore(
        SalePreparationStatus status, Instant threshold);

    @Modifying
    @Query("""
        UPDATE SalePreparationJpaEntity p
        SET p.status = com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus.EXPIRED
        WHERE p.status = com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus.DRAFT
          AND p.expiresAt < :threshold
    """)
    int expireDrafts(@Param("threshold") Instant threshold);

    @Modifying
    @Query("""
        DELETE FROM SalePreparationJpaEntity p
        WHERE p.status IN :statuses AND p.updatedAt < :threshold
    """)
    int purgeByStatusOlderThan(
        @Param("statuses") List<SalePreparationStatus> statuses,
        @Param("threshold") Instant threshold);
}
