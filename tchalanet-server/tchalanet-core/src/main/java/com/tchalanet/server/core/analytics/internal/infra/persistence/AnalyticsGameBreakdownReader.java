package com.tchalanet.server.core.analytics.internal.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Reads game-level sales breakdowns from {@code analytics_selection}.
 *
 * <p>{@code analytics_selection} is the current projection that carries stable
 * {@code game_code}. This keeps dashboard/report queries inside core.analytics
 * without bending {@code analytics_daily.dimension_id} into a non-UUID key.
 */
@Component
public class AnalyticsGameBreakdownReader {

  @PersistenceContext
  private EntityManager em;

  public List<GameBreakdownRow> findPlatformGameBreakdown(
      LocalDate from,
      LocalDate to,
      int limit) {
    return query(null, from, to, limit);
  }

  public List<GameBreakdownRow> findTenantGameBreakdown(
      UUID tenantId,
      LocalDate from,
      LocalDate to,
      int limit) {
    return query(tenantId, from, to, limit);
  }

  private List<GameBreakdownRow> query(UUID tenantId, LocalDate from, LocalDate to, int limit) {
    String tenantFilter = tenantId == null ? "" : "AND tenant_id = :tenantId";
    String sql = """
        SELECT
            game_code,
            COALESCE(SUM(tickets_count), 0) AS tickets_sold,
            COALESCE(SUM(stake_sum_cents), 0) AS gross_sales_cents,
            COALESCE(SUM(winnings_calculated_cents), 0) AS winnings_calculated_cents
        FROM analytics_selection
        WHERE ref_date BETWEEN :fromDate AND :toDate
        %s
        GROUP BY game_code
        ORDER BY gross_sales_cents DESC, game_code ASC
        LIMIT :limit
        """.formatted(tenantFilter);

    var query = em.createNativeQuery(sql)
        .setParameter("fromDate", from)
        .setParameter("toDate", to)
        .setParameter("limit", Math.max(limit, 0));
    if (tenantId != null) {
      query.setParameter("tenantId", tenantId);
    }

    @SuppressWarnings("unchecked")
    List<Object[]> rows = query.getResultList();

    return rows.stream()
        .map(r -> {
          long grossCents = ((Number) r[2]).longValue();
          long winningsCents = ((Number) r[3]).longValue();
          return new GameBreakdownRow(
              (String) r[0],
              ((Number) r[1]).longValue(),
              fromCents(grossCents),
              fromCents(grossCents - winningsCents));
        })
        .toList();
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }

  public record GameBreakdownRow(
      String gameCode,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal netRevenueEstimated) {}
}
