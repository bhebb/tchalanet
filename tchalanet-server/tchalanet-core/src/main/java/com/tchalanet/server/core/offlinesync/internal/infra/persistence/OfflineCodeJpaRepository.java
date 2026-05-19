package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfflineCodeJpaRepository extends JpaRepository<OfflineCodeJpaEntity, UUID> {

    Optional<OfflineCodeJpaEntity> findByTenantIdAndGrantIdAndCode(
        UUID tenantId, UUID grantId, String code);

    /**
     * Acquire a pessimistic write lock on a code row. Used by the sync handler to
     * atomically transition AVAILABLE → RESERVED inside a single transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OfflineCodeJpaEntity> findOneById(UUID id);

    /** Codes still RESERVED beyond a threshold — used by the orphan watchdog. */
    List<OfflineCodeJpaEntity> findAllByStatusAndReservedAtLessThan(
        String status, Instant threshold);
}
