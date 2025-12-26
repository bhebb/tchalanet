package com.tchalanet.server.core.ledger.infra.persistence.repo;

import com.tchalanet.server.core.ledger.infra.persistence.LedgerEntryJpaEntity;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringLedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {
    List<LedgerEntryJpaEntity> findByTenantIdAndOccurredAtBetweenOrderByOccurredAtDesc(UUID tenantId, Instant from, Instant to);

    List<LedgerEntryJpaEntity> findByTenantIdAndRefTypeAndRefId(UUID tenantId, LedgerRefType refType, UUID refId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END), 0) FROM LedgerEntryJpaEntity e WHERE e.tenantId = :tenantId")
    BigDecimal computeBalance(@Param("tenantId") UUID tenantId);

    boolean existsByTenantIdAndRefTypeAndRefId(UUID tenantId, LedgerRefType refType, UUID refId);
}
