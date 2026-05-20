package com.tchalanet.server.core.payout.internal.infra.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringPayoutJpaRepository extends JpaRepository<PayoutJpaEntity, UUID> {
  Optional<PayoutJpaEntity> findByTicketId(UUID ticketId);

    @Query("""
    select p
    from PayoutJpaEntity p
    where (:status is null or p.status = :status)
      and (:ticketId is null or p.ticketId = :ticketId)
      and (:outletId is null or p.payingOutletId = :outletId or p.sellingOutletId = :outletId)
      and (:sessionId is null or p.payingSessionId = :sessionId or p.sellingSessionId = :sessionId)
      and (:from is null or p.createdAt >= :from)
      and (:to is null or p.createdAt < :to)
    """)
    Page<PayoutJpaEntity> search(
        String status,
        UUID ticketId,
        UUID outletId,
        UUID sessionId,
        Instant from,
        Instant to,
        Pageable pageable);
}
