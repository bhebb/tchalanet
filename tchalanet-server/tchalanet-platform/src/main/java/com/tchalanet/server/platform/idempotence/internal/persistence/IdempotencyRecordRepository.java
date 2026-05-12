package com.tchalanet.server.platform.idempotence.internal.persistence;

import com.tchalanet.server.common.types.enums.IdempotencyScope;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface IdempotencyRecordRepository
    extends JpaRepository<IdempotencyRecordJpaEntity, UUID> {

  Optional<IdempotencyRecordJpaEntity> findByTenantIdAndScopeAndKey(
      UUID tenantId, IdempotencyScope scope, String key);

  @Modifying
  @Transactional
  @Query(
      "update IdempotencyRecordJpaEntity r "
          + "set r.status = :status, r.resourceId = :resourceId, r.responseJson = :responseJson, r.updatedAt = CURRENT_TIMESTAMP "
          + "where r.tenantId = :tenantId and r.scope = :scope and r.key = :key and r.requestHash = :requestHash")
  int markCompleted(
      @Param("tenantId") UUID tenantId,
      @Param("scope") IdempotencyScope scope,
      @Param("key") String key,
      @Param("requestHash") String requestHash,
      @Param("status") IdempotencyRecordJpaEntity.Status status,
      @Param("resourceId") UUID resourceId,
      @Param("responseJson") String responseJson);

  @Modifying
  @Transactional
  @Query("delete from IdempotencyRecordJpaEntity r where r.expiresAt < :now")
  int deleteExpired(@Param("now") Instant now);
}
