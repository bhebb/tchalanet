package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Dedicated read fragment for sync-window-close candidates. */
public interface SyncAcceptedWindowQueryRepository
    extends Repository<OfflineSubmissionJpaEntity, UUID> {

    @Query("""
        SELECT s FROM OfflineSubmissionJpaEntity s, OfflineGrantJpaEntity g
        WHERE s.grantId = g.id
          AND s.status = 'RECEIVED'
          AND g.syncAcceptedUntil < :now
        """)
    List<OfflineSubmissionJpaEntity> findReceivedAfterWindowClosed(@Param("now") Instant now);
}

