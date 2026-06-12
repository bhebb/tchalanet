package com.tchalanet.server.core.analytics.internal.infra.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AnalyticsSessionRepository extends JpaRepository<AnalyticsSessionEntity, UUID>,
    AnalyticsSessionOpsRepository {}


interface AnalyticsSessionOpsRepository {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void ensureOpen(UUID sessionId, UUID tenantId, UUID outletId, UUID terminalId,
      UUID sellerUserId, LocalDate businessDate, Instant openedAt);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void closeSession(UUID sessionId, Instant closedAt);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void increment(UUID sessionId,
      long ticketsSoldDelta, long ticketsVoidedDelta,
      long grossSalesDelta, long stakeTotalDelta, long payoutsPaidDelta);
}


@Repository
class AnalyticsSessionOpsRepositoryImpl implements AnalyticsSessionOpsRepository {

  @PersistenceContext
  private EntityManager em;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void ensureOpen(UUID sessionId, UUID tenantId, UUID outletId, UUID terminalId,
      UUID sellerUserId, LocalDate businessDate, Instant openedAt) {

    em.createNativeQuery("SELECT public.ensure_analytics_session("
            + ":sid, :tid, :oid, :termId, :sellerId, :bd, :oa)")
        .setParameter("sid",    sessionId)
        .setParameter("tid",    tenantId)
        .setParameter("oid",    outletId)
        .setParameter("termId", terminalId)
        .setParameter("sellerId", sellerUserId)
        .setParameter("bd",     businessDate)
        .setParameter("oa",     openedAt)
        .getSingleResult();
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void closeSession(UUID sessionId, Instant closedAt) {
    em.createNativeQuery("SELECT public.close_analytics_session(:sid, :ca)")
        .setParameter("sid", sessionId)
        .setParameter("ca",  closedAt)
        .getSingleResult();
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void increment(UUID sessionId,
      long ticketsSoldDelta, long ticketsVoidedDelta,
      long grossSalesDelta, long stakeTotalDelta, long payoutsPaidDelta) {

    em.createNativeQuery("SELECT public.increment_analytics_session("
            + ":sid, :ts, :tv, :gs, :st, :pp)")
        .setParameter("sid", sessionId)
        .setParameter("ts",  ticketsSoldDelta)
        .setParameter("tv",  ticketsVoidedDelta)
        .setParameter("gs",  grossSalesDelta)
        .setParameter("st",  stakeTotalDelta)
        .setParameter("pp",  payoutsPaidDelta)
        .getSingleResult();
  }
}
