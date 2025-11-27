package com.tchalanet.server.pos.infra.persistence.repository;

import com.tchalanet.server.pos.domain.model.PosSessionStatus;
import com.tchalanet.server.pos.infra.persistence.entity.PosSessionEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringPosSessionJpaRepository extends JpaRepository<PosSessionEntity, UUID> {
  Optional<PosSessionEntity> findByTenantIdAndTerminalIdAndStatus(
      UUID tenantId, UUID terminalId, PosSessionStatus status);

  @Query(
      "SELECT s FROM PosSessionEntity s WHERE s.status = 'OPEN' AND s.lastActivityAt < :idleCutoff AND s.openedAt < :openedCutoff")
  List<PosSessionEntity> findOpenSessionsToAutoClose(
      @Param("idleCutoff") Instant idleCutoff, @Param("openedCutoff") Instant openedCutoff);
}
