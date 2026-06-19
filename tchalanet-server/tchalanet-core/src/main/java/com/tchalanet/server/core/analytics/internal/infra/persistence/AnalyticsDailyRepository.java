package com.tchalanet.server.core.analytics.internal.infra.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@code analytics_daily}.
 *
 * <p>Read queries use JPQL/Spring Data; the upsert uses the SQL function
 * {@code public.upsert_analytics_daily} defined in V109 migration.
 */
@Repository
public interface AnalyticsDailyRepository extends JpaRepository<AnalyticsDailyEntity, UUID>,
    AnalyticsDailyUpsertRepository {

  // ── read queries ──────────────────────────────────────────────────────────

  /** Sum all TENANT rows for a tenant over a date range. */
  @Query("""
      SELECT a FROM AnalyticsDailyEntity a
       WHERE a.dimensionType = 'TENANT'
         AND a.tenantId = :tenantId
         AND a.refDate BETWEEN :from AND :to
       ORDER BY a.refDate
      """)
  List<AnalyticsDailyEntity> findTenantRows(
      @Param("tenantId") UUID tenantId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /** Single PLATFORM row for a date (global rollup). */
  @Query("""
      SELECT a FROM AnalyticsDailyEntity a
       WHERE a.dimensionType = 'PLATFORM'
         AND a.dimensionId IS NULL
         AND a.tenantId IS NULL
         AND a.refDate = :refDate
      """)
  Optional<AnalyticsDailyEntity> findPlatformRow(@Param("refDate") LocalDate refDate);

  /** PLATFORM rows for a date range. */
  @Query("""
      SELECT a FROM AnalyticsDailyEntity a
       WHERE a.dimensionType = 'PLATFORM'
         AND a.dimensionId IS NULL
         AND a.tenantId IS NULL
         AND a.refDate BETWEEN :from AND :to
       ORDER BY a.refDate
      """)
  List<AnalyticsDailyEntity> findPlatformRows(
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /** All TENANT rows for a date range (for top-tenant ranking). */
  @Query("""
      SELECT a FROM AnalyticsDailyEntity a
       WHERE a.dimensionType = 'TENANT'
         AND a.refDate BETWEEN :from AND :to
       ORDER BY a.grossSalesCents DESC
      """)
  List<AnalyticsDailyEntity> findAllTenantRows(
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /** SELLER dimension rows for a specific user+tenant+date. */
  @Query("""
      SELECT a FROM AnalyticsDailyEntity a
       WHERE a.dimensionType = 'SELLER'
         AND a.tenantId = :tenantId
         AND a.dimensionId = :sellerId
         AND a.refDate = :refDate
      """)
  Optional<AnalyticsDailyEntity> findSellerRow(
      @Param("tenantId") UUID tenantId,
      @Param("sellerTerminalId1") UUID sellerId,
      @Param("refDate") LocalDate refDate);

  /** Delete rows older than retention cutoff for purge. */
  @Transactional
  @org.springframework.data.jpa.repository.Modifying
  @Query("DELETE FROM AnalyticsDailyEntity a WHERE a.refDate < :cutoff")
  int deleteOlderThan(@Param("cutoff") LocalDate cutoff);

  /** Count rows older than retention cutoff (dry-run). */
  @Query("SELECT COUNT(a) FROM AnalyticsDailyEntity a WHERE a.refDate < :cutoff")
  long countOlderThan(@Param("cutoff") LocalDate cutoff);
}


/**
 * Custom interface for the native SQL upsert (separate from JPA repository to keep
 * Spring Data from trying to parse the SQL as JPQL).
 */
interface AnalyticsDailyUpsertRepository {

  /**
   * Atomic upsert via {@code public.upsert_analytics_daily(...)}.
   *
   * <p>Uses {@link Propagation#REQUIRES_NEW} to ensure the upsert runs in
   * its own transaction, avoiding lock contention with the outer event listener
   * transaction that already committed.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void upsertAndIncrement(
      String dimensionType,
      UUID dimensionId,
      UUID tenantId,
      LocalDate refDate,
      long ticketsSoldDelta,
      long ticketsCancelledDelta,
      long grossSalesDelta,
      long stakeTotalDelta,
      long winningsCalcDelta,
      long payoutsPaidDelta,
      long sessionsOpenedDelta,
      long sessionsClosedDelta);
}


/**
 * Implementation of the custom upsert.
 * Named with suffix {@code Impl} per Spring Data convention.
 */
@Repository
class AnalyticsDailyUpsertRepositoryImpl implements AnalyticsDailyUpsertRepository {

  @PersistenceContext
  private EntityManager em;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void upsertAndIncrement(
      String dimensionType, UUID dimensionId, UUID tenantId, LocalDate refDate,
      long ticketsSoldDelta, long ticketsCancelledDelta,
      long grossSalesDelta, long stakeTotalDelta,
      long winningsCalcDelta, long payoutsPaidDelta,
      long sessionsOpenedDelta, long sessionsClosedDelta) {

    em.createNativeQuery("SELECT public.upsert_analytics_daily("
            + ":dt, :dimId, :tid, :rd, :ts, :tc, :gs, :st, :wc, :pp, :so, :sc)")
        .setParameter("dt",   dimensionType)
        .setParameter("dimId", dimensionId)
        .setParameter("tid",   tenantId)
        .setParameter("rd",    refDate)
        .setParameter("ts",    ticketsSoldDelta)
        .setParameter("tc",    ticketsCancelledDelta)
        .setParameter("gs",    grossSalesDelta)
        .setParameter("st",    stakeTotalDelta)
        .setParameter("wc",    winningsCalcDelta)
        .setParameter("pp",    payoutsPaidDelta)
        .setParameter("so",    sessionsOpenedDelta)
        .setParameter("sc",    sessionsClosedDelta)
        .getSingleResult();
  }
}
