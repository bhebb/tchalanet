package com.tchalanet.server.platform.communication.internal.persistence;

import com.tchalanet.server.platform.communication.api.model.value.DeliveryStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboundMessageJpaRepository extends JpaRepository<OutboundMessageJpaEntity, UUID> {

  @Query(
      """
      select m from OutboundMessageJpaEntity m
       where m.deletedAt is null
         and (:tenantId is null or m.tenantId = :tenantId)
         and m.correlationKey = :correlationKey
      """)
  Optional<OutboundMessageJpaEntity> findByTenantAndCorrelationKey(
      @Param("tenantId") UUID tenantId,
      @Param("correlationKey") String correlationKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select m from OutboundMessageJpaEntity m
       where m.deletedAt is null
         and m.status = :status
         and (m.nextAttemptAt is null or m.nextAttemptAt <= :now)
       order by m.priority desc, m.createdAt asc
      """)
  List<OutboundMessageJpaEntity> findDueForDispatch(
      @Param("status") DeliveryStatus status,
      @Param("now") Instant now,
      Pageable pageable);
}
