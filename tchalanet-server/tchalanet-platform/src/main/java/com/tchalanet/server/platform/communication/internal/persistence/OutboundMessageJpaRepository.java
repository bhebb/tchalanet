package com.tchalanet.server.platform.communication.internal.persistence;

import com.tchalanet.server.platform.communication.api.model.value.DeliveryStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
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

  @Query(
      """
      select m from OutboundMessageJpaEntity m
       where m.deletedAt is null
         and (:status is null or m.status = :status)
         and (:channel is null or m.channel = :channel)
         and (:tenantId is null or m.tenantId = :tenantId)
         and (:recipient is null or lower(m.recipientValue) like lower(concat('%', :recipient, '%')))
       order by m.createdAt desc
      """)
  Page<OutboundMessageJpaEntity> searchOpsMessages(
      @Param("status") DeliveryStatus status,
      @Param("channel") com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel channel,
      @Param("tenantId") UUID tenantId,
      @Param("recipient") String recipient,
      Pageable pageable);

  long countByDeletedAtIsNullAndStatus(DeliveryStatus status);
}
