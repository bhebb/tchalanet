package com.tchalanet.server.features.stats.aggregates.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SalesReportingReader {

  @PersistenceContext private EntityManager em;

  public record DailyTenantStatsRow(
      UUID tenantId,
      LocalDate refDate,
      long ticketsCount,
      long ticketsCancelledCount,
      long stakeSumCents,
      long winningsSumCents,
      long netRevenueCents,
      long payoutsCount,
      long sessionsOpenedCount,
      long sessionsClosedCount) {}

  public List<DailyTenantStatsRow> listDailyTenantStats(LocalDate fromDate, LocalDate toDate) {
    String sql =
        """
            select
              t.tenant_id::uuid as tenant_id,
              date_trunc('day', t.sold_at)::date as ref_date,
              count(*) filter (where t.status <> 'CANCELLED') as tickets_count,
              count(*) filter (where t.status = 'CANCELLED') as tickets_cancelled_count,
              coalesce(sum((t.total_amount * 100)::bigint),0) as stake_sum_cents,
              coalesce(sum((t.total_payout * 100)::bigint),0) as winnings_sum_cents,
              coalesce(sum(((t.total_amount - t.total_payout) * 100)::bigint),0) as net_revenue_cents,
              0 as payouts_count,
              0 as sessions_opened_count,
              0 as sessions_closed_count
            from ticket t
            where t.sold_at >= :fromDate
              and t.sold_at < (:toDate + interval '1 day')
            group by t.tenant_id, ref_date
            """;

    var query = em.createNativeQuery(sql);
    query.setParameter("fromDate", Date.valueOf(fromDate));
    query.setParameter("toDate", Date.valueOf(toDate));

    @SuppressWarnings("unchecked")
    List<Object[]> rows = query.getResultList();

    return rows.stream()
        .map(
            r ->
                new DailyTenantStatsRow(
                    (UUID) r[0],
                    ((Date) r[1]).toLocalDate(),
                    ((Number) r[2]).longValue(),
                    ((Number) r[3]).longValue(),
                    ((Number) r[4]).longValue(),
                    ((Number) r[5]).longValue(),
                    ((Number) r[6]).longValue(),
                    ((Number) r[7]).longValue(),
                    ((Number) r[8]).longValue(),
                    ((Number) r[9]).longValue()))
        .toList();
  }
}
