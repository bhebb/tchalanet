package com.tchalanet.server.audit.infra.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AuditEventSpringRepository extends JpaRepository<AuditEventJpaEntity, UUID> {
  List<AuditEventJpaEntity> findTop100ByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID tenantId);

  @Modifying
  @Transactional
  @Query("delete from AuditEventJpaEntity e where e.occurredAt < :threshold")
  int deleteByOccurredAtBefore(@Param("threshold") Instant threshold);
}
