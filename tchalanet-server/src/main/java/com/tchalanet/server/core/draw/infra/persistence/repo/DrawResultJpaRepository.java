package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DrawResultJpaRepository extends JpaRepository<DrawResultJpaEntity, UUID> {
    Optional<DrawResultJpaEntity> findByTenantIdAndDrawId(UUID tenantId, UUID drawId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO draw_result (id, tenant_id, draw_id, source, status, numbers_main, numbers_extra, raw_payload, occurred_at, created_at, updated_at) VALUES (:id, :tenantId, :drawId, :source, :status, :numbersMain, :numbersExtra, cast(:rawPayload as jsonb), :occurredAt, now(), now()) ON CONFLICT (tenant_id, draw_id) DO UPDATE SET source = EXCLUDED.source, status = EXCLUDED.status, numbers_main = EXCLUDED.numbers_main, numbers_extra = EXCLUDED.numbers_extra, raw_payload = EXCLUDED.raw_payload, occurred_at = EXCLUDED.occurred_at, updated_at = now()",
        nativeQuery = true)
    int upsertResult(
        @Param("id") UUID id,
        @Param("tenantId") UUID tenantId,
        @Param("drawId") UUID drawId,
        @Param("source") String source,
        @Param("status") String status,
        @Param("numbersMain") Object numbersMain,
        @Param("numbersExtra") Object numbersExtra,
        @Param("rawPayload") String rawPayload,
        @Param("occurredAt") Instant occurredAt);
}
