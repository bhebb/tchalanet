package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfflineGrantJpaRepository extends JpaRepository<OfflineGrantJpaEntity, UUID> {

    Optional<OfflineGrantJpaEntity>
        findFirstBySellerUserIdAndTerminalIdAndDeviceIdAndStatusOrderByIssuedAtDesc(
            UUID sellerUserId, UUID terminalId, UUID deviceId, String status);

    /** Grants of the given status whose sync window has elapsed. Used by expiration job. */
    List<OfflineGrantJpaEntity> findAllByStatusAndSyncAcceptedUntilLessThan(
        String status, Instant threshold);

    /** Pessimistic-write reload — used to serialise concurrent quota updates. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM OfflineGrantJpaEntity g WHERE g.id = :id")
    Optional<OfflineGrantJpaEntity> findOneByIdForUpdate(@Param("id") UUID id);
}
