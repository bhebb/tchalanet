package com.tchalanet.server.core.analytics.internal.infra.persistence;

import com.tchalanet.server.core.analytics.api.model.SalesReportLine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Reads per-day per-game sales breakdown from {@code analytics_draw}.
 *
 * <p>Groups draw-level aggregates by {@code ref_date + game_code} to produce
 * a daily sales report. This avoids re-reading the live ticket table.
 */
@Component
public class SalesReportAnalyticsReader {

  @PersistenceContext
  private EntityManager em;

  public List<SalesReportLine> findSalesByPeriodAndGame(
      UUID tenantId, LocalDate fromDate, LocalDate toDate, String gameCode) {

    String sql =
        """
        SELECT
            ref_date                                         AS date,
            game_code,
            COALESCE(SUM(tickets_sold_count), 0)             AS tickets_sold,
            COALESCE(SUM(gross_sales_cents), 0)              AS gross_sales_cents,
            COALESCE(SUM(payouts_paid_cents), 0)             AS payouts_paid_cents,
            COALESCE(SUM(net_revenue_estimated_cents), 0)    AS net_revenue_cents
        FROM analytics_draw
        WHERE tenant_id = :tenantId
          AND ref_date BETWEEN :fromDate AND :toDate
          AND (CAST(:gameCode AS varchar) IS NULL OR game_code = CAST(:gameCode AS varchar))
        GROUP BY ref_date, game_code
        ORDER BY ref_date ASC, game_code ASC
        """;

    @SuppressWarnings("unchecked")
    List<Object[]> rows = em.createNativeQuery(sql)
        .setParameter("tenantId", tenantId)
        .setParameter("fromDate", fromDate)
        .setParameter("toDate", toDate)
        .setParameter("gameCode", gameCode)
        .getResultList();

    return rows.stream()
        .map(r -> new SalesReportLine(
            ((Date) r[0]).toLocalDate(),
            (String) r[1],
            ((Number) r[2]).longValue(),
            fromCents(((Number) r[3]).longValue()),
            fromCents(((Number) r[4]).longValue()),
            fromCents(((Number) r[5]).longValue())))
        .toList();
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }
}
