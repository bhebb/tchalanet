package com.tchalanet.server.core.session.infra.persistence.repository;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaPayoutAggregateRepository implements PayoutAggregateRepository {

  private final EntityManager em;

  @Override
  public BigDecimal computePayoutAgg(UUID tenantId, UUID sessionId) {
    Object val =
        em.createNativeQuery(
                """
        SELECT COALESCE(SUM(p.amount), 0)
        FROM payout p
        WHERE p.tenant_id = :tenantId
          AND p.session_id = :sessionId
          AND p.deleted_at IS NULL
          AND p.status IN ('PAID','APPROVED') -- adapte
      """)
            .setParameter("tenantId", tenantId)
            .setParameter("sessionId", sessionId)
            .getSingleResult();

    return (BigDecimal) val;
  }
}
