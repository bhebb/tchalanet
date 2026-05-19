package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OfflineEventOutboxJpaRepository
    extends JpaRepository<OfflineEventOutboxJpaEntity, UUID> {

    /**
     * Pull a batch of pending outbox rows ready for publication. Uses a SKIP LOCKED scan so
     * multiple drainer instances do not pick the same row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o FROM OfflineEventOutboxJpaEntity o
        WHERE o.publishedAt IS NULL
          AND (o.nextAttemptAt IS NULL OR o.nextAttemptAt <= :now)
        ORDER BY o.createdAt ASC
        """)
    List<OfflineEventOutboxJpaEntity> findPendingForPublish(
        @Param("now") Instant now, Pageable pageable);
}
