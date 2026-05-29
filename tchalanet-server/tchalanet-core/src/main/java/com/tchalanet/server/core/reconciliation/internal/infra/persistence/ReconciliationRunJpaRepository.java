package com.tchalanet.server.core.reconciliation.internal.infra.persistence;

import java.util.List;
import java.util.Optional;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationRunType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReconciliationRunJpaRepository extends JpaRepository<ReconciliationRunJpaEntity, UUID> {
    List<ReconciliationRunJpaEntity> findByTenantIdOrderByStartedAtDesc(UUID tenantId);

    Optional<ReconciliationRunJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndBusinessDateAndRunType(UUID tenantId, java.time.LocalDate businessDate, ReconciliationRunType runType);
}
