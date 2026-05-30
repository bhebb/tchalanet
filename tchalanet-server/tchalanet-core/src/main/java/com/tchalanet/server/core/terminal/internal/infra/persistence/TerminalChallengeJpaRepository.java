package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TerminalChallengeJpaRepository extends TchJpaRepository<TerminalChallengeJpaEntity, UUID> {

    Optional<TerminalChallengeJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @Modifying
    @Query("""
        UPDATE TerminalChallengeJpaEntity c
           SET c.status = :cancelled, c.cancelledAt = :now
         WHERE c.tenantId = :tenantId
           AND c.terminalId = :terminalId
           AND c.userId = :userId
           AND c.challengeType = :type
           AND c.status = :pending
        """)
    int cancelAllPending(
        @Param("tenantId") UUID tenantId,
        @Param("terminalId") UUID terminalId,
        @Param("userId") UUID userId,
        @Param("type") TerminalChallengeType type,
        @Param("pending") TerminalChallengeStatus pending,
        @Param("cancelled") TerminalChallengeStatus cancelled,
        @Param("now") Instant now
    );
}
