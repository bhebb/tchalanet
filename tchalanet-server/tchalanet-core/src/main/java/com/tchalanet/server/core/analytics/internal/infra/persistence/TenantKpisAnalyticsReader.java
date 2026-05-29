package com.tchalanet.server.core.analytics.internal.infra.persistence;

import com.tchalanet.server.core.analytics.api.model.TenantKpisView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Reads aggregated tenant KPIs from the {@code analytics_daily} projection tables.
 *
 * <p>Active outlets and active cashiers are computed by counting distinct
 * OUTLET and SELLER dimension rows in the period — a proxy for "did this entity sell anything".
 *
 * <p>Architecture: only reads from analytics_daily — owned by core.analytics.
 */
@Component
public class TenantKpisAnalyticsReader {

  @PersistenceContext
  private EntityManager em;

  public TenantKpisView computeTenantKpis(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
    String summarySql =
        """
        SELECT
            COALESCE(SUM(tickets_sold_count), 0)          AS tickets_sold,
            COALESCE(SUM(gross_sales_cents), 0)            AS gross_sales_cents,
            COALESCE(SUM(payouts_paid_cents), 0)           AS payouts_paid_cents,
            COALESCE(SUM(net_revenue_estimated_cents), 0)  AS net_revenue_cents
        FROM analytics_daily
        WHERE dimension_type = 'TENANT'
          AND tenant_id = :tenantId
          AND ref_date BETWEEN :fromDate AND :toDate
        """;

    Object[] summary = (Object[]) em.createNativeQuery(summarySql)
        .setParameter("tenantId", tenantId)
        .setParameter("fromDate", fromDate)
        .setParameter("toDate", toDate)
        .getSingleResult();

    long ticketsSold      = ((Number) summary[0]).longValue();
    BigDecimal totalSales = fromCents(((Number) summary[1]).longValue());
    BigDecimal totalPayout= fromCents(((Number) summary[2]).longValue());
    BigDecimal netRevenue = fromCents(((Number) summary[3]).longValue());

    // Active outlets = distinct OUTLET dimension rows with at least 1 ticket sold
    String outletSql =
        """
        SELECT COUNT(DISTINCT dimension_id)
        FROM analytics_daily
        WHERE dimension_type = 'OUTLET'
          AND tenant_id = :tenantId
          AND ref_date BETWEEN :fromDate AND :toDate
          AND tickets_sold_count > 0
        """;
    long activeOutlets = ((Number) em.createNativeQuery(outletSql)
        .setParameter("tenantId", tenantId)
        .setParameter("fromDate", fromDate)
        .setParameter("toDate", toDate)
        .getSingleResult()).longValue();

    // Active cashiers = distinct SELLER dimension rows with at least 1 ticket sold
    String sellerSql =
        """
        SELECT COUNT(DISTINCT dimension_id)
        FROM analytics_daily
        WHERE dimension_type = 'SELLER'
          AND tenant_id = :tenantId
          AND ref_date BETWEEN :fromDate AND :toDate
          AND tickets_sold_count > 0
        """;
    long activeCashiers = ((Number) em.createNativeQuery(sellerSql)
        .setParameter("tenantId", tenantId)
        .setParameter("fromDate", fromDate)
        .setParameter("toDate", toDate)
        .getSingleResult()).longValue();

    return new TenantKpisView(ticketsSold, totalSales, totalPayout, netRevenue,
        activeOutlets, activeCashiers);
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }
}
