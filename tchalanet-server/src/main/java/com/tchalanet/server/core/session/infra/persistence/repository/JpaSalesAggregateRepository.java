package com.tchalanet.server.core.session.infra.persistence.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JpaSalesAggregateRepository implements SalesAggregateRepository {

  private final EntityManager em;

  @Override
  public SalesAgg computeTicketAgg(UUID tenantId, UUID sessionId) {
    Object[] row = (Object[]) em.createNativeQuery("""
        SELECT
          COALESCE(COUNT(*), 0) as total_tickets,
          COALESCE(SUM(t.total_amount), 0) as total_stake
        FROM ticket t
        WHERE t.tenant_id = :tenantId
          AND t.session_id = :sessionId
          AND t.deleted_at IS NULL
          AND t.status NOT IN ('VOID') -- adapte selon ton enum DB
      """)
      .setParameter("tenantId", tenantId)
      .setParameter("sessionId", sessionId)
      .getSingleResult();

    long tickets = ((Number) row[0]).longValue();
    BigDecimal stake = (BigDecimal) row[1];
    return new SalesAgg(tickets, stake);
  }
}
