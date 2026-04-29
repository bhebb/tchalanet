package com.tchalanet.server.features.reporting.outletperformance;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutletPerformanceReader {

  private final EntityManager em;

  public List<OutletPerformanceLine> findOutletPerformance(
      UUID tenantId, LocalDate fromDate, LocalDate toDate, String gameCode // null => tous jeux
      ) {
    LocalDate toExclusive = toDate.plusDays(1);

    var sql =
        """
                select
                    o.id                                        as outlet_id,
                    o.code                                      as outlet_code,
                    o.name                                      as outlet_name,
                    t.game_code                                 as game_code,
                    count(*)                                    as tickets_sold,
                    coalesce(sum(t.total_amount), 0)           as total_sales,
                    coalesce(sum(t.total_payout), 0)           as total_payout,
                    coalesce(sum(t.total_amount - t.total_payout), 0) as net_revenue
                from ticket t
                join outlet o on o.id = t.outlet_id
                where t.tenant_id = :tenantId
                  and t.sold_at >= :fromDate
                  and t.sold_at < :toExclusive
                  and (:gameCode is null or t.game_code = :gameCode)
                group by o.id, o.code, o.name, t.game_code
                order by total_sales desc
                """;

    var query = em.createNativeQuery(sql);
    query.setParameter("tenantId", tenantId);
    query.setParameter("fromDate", fromDate);
    query.setParameter("toExclusive", toExclusive);
    query.setParameter("gameCode", gameCode);

    @SuppressWarnings("unchecked")
    List<Object[]> rows = query.getResultList();

    return rows.stream()
        .map(
            r ->
                new OutletPerformanceLine(
                    (UUID) r[0],
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    ((Number) r[4]).longValue(),
                    (BigDecimal) r[5],
                    (BigDecimal) r[6],
                    (BigDecimal) r[7]))
        .toList();
  }
}
