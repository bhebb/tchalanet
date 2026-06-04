package com.tchalanet.server.core.payout.internal.infra.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringPayoutJpaRepository extends JpaRepository<PayoutJpaEntity, UUID> {
  Optional<PayoutJpaEntity> findByTicketId(UUID ticketId);

    List<PayoutJpaEntity> findByTicketIdIn(List<UUID> ticketIds);

    Optional<PayoutJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    // PostgreSQL + Hibernate 6 cannot infer the type of a null literal in
    // "(:param is null or col = :param)". Wrapping each nullable parameter in
    // cast() forces the driver to emit a typed CAST and resolves the
    // "could not determine data type of parameter $N" error.
    @Query("""
    select p
    from PayoutJpaEntity p
    where (cast(:status as String) is null or p.status = :status)
      and (cast(:ticketId as java.util.UUID) is null or p.ticketId = :ticketId)
      and (cast(:outletId as java.util.UUID) is null or p.payingOutletId = :outletId or p.sellingOutletId = :outletId)
      and (cast(:sessionId as java.util.UUID) is null or p.payingSessionId = :sessionId or p.sellingSessionId = :sessionId)
      and (cast(:from as java.time.Instant) is null or p.createdAt >= :from)
      and (cast(:to as java.time.Instant) is null or p.createdAt < :to)
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
