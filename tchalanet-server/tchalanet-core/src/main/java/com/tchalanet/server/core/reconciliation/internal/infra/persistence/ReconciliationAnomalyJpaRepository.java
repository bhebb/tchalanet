package com.tchalanet.server.core.reconciliation.internal.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationAnomalyJpaRepository extends JpaRepository<ReconciliationAnomalyJpaEntity, UUID> {
    Optional<ReconciliationAnomalyJpaEntity> findByTenantIdAndFingerprint(UUID tenantId, String fingerprint);

    List<ReconciliationAnomalyJpaEntity> findByTenantIdAndRunIdOrderBySeverityAscAnomalyTypeAsc(UUID tenantId, UUID runId);
}
