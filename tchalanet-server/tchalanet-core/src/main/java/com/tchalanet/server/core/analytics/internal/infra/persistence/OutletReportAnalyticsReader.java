package com.tchalanet.server.core.analytics.internal.infra.persistence;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.analytics.api.model.OutletReportLine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Reads outlet performance metrics for a tenant.
 *
 * <p>When no game filter is applied: uses {@code analytics_daily} (OUTLET dimension)
 * for aggregated totals — fast, projection-based.
 *
 * <p>When a game filter is applied: falls back to {@code ticket JOIN outlet} (source tables)
 * because {@code analytics_daily} does not carry game-code breakdown at outlet granularity.
 * TODO V2: add OUTLET+GAME composite rows to analytics_daily or analytics_draw.
 */
@Component
public class OutletReportAnalyticsReader {

  @PersistenceContext
  private EntityManager em;

  public List<OutletReportLine> findOutletPerformance(
      UUID tenantId, LocalDate fromDate, LocalDate toDate, String gameCode) {

    if (gameCode != null) {
      return findWithGameFilter(tenantId, fromDate, toDate, gameCode);
    }
    return findAllGames(tenantId, fromDate, toDate);
  }

  /** Per-outlet totals (all games) from analytics_daily projection. */
  private List<OutletReportLine> findAllGames(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
    String sql =
        """
        SELECT
            o.id                                              AS outlet_id,
            o.code                                           AS outlet_code,
            o.name                                           AS outlet_name,
            NULL::varchar                                    AS game_code,
            COALESCE(SUM(d.tickets_sold_count), 0)           AS tickets_sold,
            COALESCE(SUM(d.gross_sales_cents), 0)            AS gross_sales_cents,
            COALESCE(SUM(d.payouts_paid_cents), 0)           AS payouts_paid_cents,
            COALESCE(SUM(d.net_revenue_estimated_cents), 0)  AS net_revenue_cents
        FROM analytics_daily d
        JOIN outlet o ON o.id = d.dimension_id
        WHERE d.dimension_type = 'OUTLET'
          AND d.tenant_id = :tenantId
          AND d.ref_date BETWEEN :fromDate AND :toDate
        GROUP BY o.id, o.code, o.name
        ORDER BY gross_sales_cents DESC
        """;

    @SuppressWarnings("unchecked")
    List<Object[]> rows = em.createNativeQuery(sql)
        .setParameter("tenantId", tenantId)
        .setParameter("fromDate", fromDate)
        .setParameter("toDate", toDate)
        .getResultList();

    return toLinesCents(rows);
  }

  /**
   * Per-outlet per-game breakdown — queries source ticket table.
   * analytics_daily does not carry outlet+game composite granularity.
   */
  private List<OutletReportLine> findWithGameFilter(
      UUID tenantId, LocalDate fromDate, LocalDate toDate, String gameCode) {

    LocalDate toExclusive = toDate.plusDays(1);
    String sql =
        """
        SELECT
            o.id                                                      AS outlet_id,
            o.code                                                    AS outlet_code,
            o.name                                                    AS outlet_name,
            t.game_code,
            COUNT(*)                                                  AS tickets_sold,
            COALESCE(SUM(t.total_amount), 0)                         AS gross_sales_cents,
            COALESCE(SUM(t.total_payout), 0)                         AS payouts_paid_cents,
            COALESCE(SUM(t.total_amount - t.total_payout), 0)        AS net_revenue_cents
        FROM ticket t
        JOIN outlet o ON o.id = t.outlet_id
        WHERE t.tenant_id = :tenantId
          AND t.sold_at >= :fromDate
          AND t.sold_at < :toExclusive
          AND t.game_code = :gameCode
        GROUP BY o.id, o.code, o.name, t.game_code
        ORDER BY gross_sales_cents DESC
        """;

    @SuppressWarnings("unchecked")
    List<Object[]> rows = em.createNativeQuery(sql)
        .setParameter("tenantId", tenantId)
        .setParameter("fromDate", fromDate)
        .setParameter("toExclusive", toExclusive)
        .setParameter("gameCode", gameCode)
        .getResultList();

    return toLinesDecimal(rows);
  }

  /** Convert analytics_daily cents (bigint) to currency BigDecimal. */
  private static List<OutletReportLine> toLinesCents(List<Object[]> rows) {
    return rows.stream()
        .map(r -> new OutletReportLine(
            OutletId.of((UUID) r[0]),
            (String) r[1],
            (String) r[2],
            (String) r[3],
            ((Number) r[4]).longValue(),
            fromCents(((Number) r[5]).longValue()),
            fromCents(((Number) r[6]).longValue()),
            fromCents(((Number) r[7]).longValue())))
        .toList();
  }

  /** Convert ticket-table NUMERIC amounts (already currency) to BigDecimal. */
  private static List<OutletReportLine> toLinesDecimal(List<Object[]> rows) {
    return rows.stream()
        .map(r -> new OutletReportLine(
            OutletId.of((UUID) r[0]),
            (String) r[1],
            (String) r[2],
            (String) r[3],
            ((Number) r[4]).longValue(),
            toDecimal(r[5]),
            toDecimal(r[6]),
            toDecimal(r[7])))
        .toList();
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }

  private static BigDecimal toDecimal(Object o) {
    if (o == null) return BigDecimal.ZERO;
    if (o instanceof BigDecimal bd) return bd;
    if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
    return new BigDecimal(o.toString());
  }
}
