package com.tchalanet.server.features.reporting.salesreport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SalesReportRepository {

  @PersistenceContext private EntityManager em;

  public List<SalesReportLineDto> findSalesByPeriodAndGame(GetSalesReportByPeriodAndGameQuery q) {
    var fromDate = q.fromDate();
    var toExclusive = q.toDate().plusDays(1);

    var sql =
        """
            select
                date_trunc('day', t.sold_at)::date                as date,
                t.game_code                                       as game_code,
                count(*)                                          as tickets_sold,
                coalesce(sum(t.total_amount), 0)                  as total_sales,
                coalesce(sum(t.total_payout), 0)                  as total_payout,
                coalesce(sum(t.total_amount - t.total_payout), 0) as net_revenue
            from ticket t
            where t.tenant_id = :tenantId
              and t.sold_at >= :fromDate
              and t.sold_at < :toExclusive
              and (:gameCode is null or t.game_code = :gameCode)
            group by date, game_code
            order by date asc, game_code asc
            """;

    var query = em.createNativeQuery(sql);
    query.setParameter("tenantId", q.tenantId());
    query.setParameter("fromDate", fromDate);
    query.setParameter("toExclusive", toExclusive);
    query.setParameter("gameCode", q.gameCode());

    @SuppressWarnings("unchecked")
    List<Object[]> rows = query.getResultList();

    return rows.stream()
        .map(
            r ->
                new SalesReportLineDto(
                    ((Date) r[0]).toLocalDate(),
                    (String) r[1],
                    ((Number) r[2]).longValue(),
                    toBigDecimal(r[3]),
                    toBigDecimal(r[4]),
                    toBigDecimal(r[5])))
        .toList();
  }

  private BigDecimal toBigDecimal(Object o) {
    if (o == null) {
      return BigDecimal.ZERO;
    }
    if (o instanceof BigDecimal bd) {
      return bd;
    }
    if (o instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    return new BigDecimal(o.toString());
  }
}
