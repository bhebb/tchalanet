package com.tchalanet.server.features.reporting.tenantkpis;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class GetTenantKpisRepository {
  @PersistenceContext private EntityManager em;

  public KpisDto computeTenantKpis(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
    // TODO: adapte table/colonnes (ticket, ticket_payout, etc.)
    String sql =
        """
            select
                coalesce(count(*), 0)                                         as tickets_sold,
                coalesce(sum(t.total_amount), 0)                              as total_sales,
                coalesce(sum(t.total_payout), 0)                              as total_payout,
                coalesce(sum(t.total_amount - t.total_payout), 0)             as net_revenue,
                coalesce(count(distinct t.outlet_id), 0)                      as active_outlets,
                coalesce(count(distinct t.seller_id), 0)                      as active_cashiers
            from ticket t
            where t.tenant_id = :tenantId
              and t.sold_at >= :fromDate
              and t.sold_at < (:toDate + interval '1 day')
            """;

    var query = em.createNativeQuery(sql);
    query.setParameter("tenantId", tenantId);
    query.setParameter("fromDate", LocalDate.from(fromDate));
    query.setParameter("toDate", LocalDate.from(toDate));

    Object[] row = (Object[]) query.getSingleResult();

    long ticketsSold = ((Number) row[0]).longValue();
    BigDecimal totalSales = toBigDecimal(row[1]);
    BigDecimal totalPayout = toBigDecimal(row[2]);
    BigDecimal netRevenue = toBigDecimal(row[3]);
    long activeOutlets = ((Number) row[4]).longValue();
    long activeCashiers = ((Number) row[5]).longValue();

    return new KpisDto(
        ticketsSold,
        totalSales,
        totalPayout,
        netRevenue,
        activeOutlets,
        activeCashiers,
        null,
        null,
        0,
        null);
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
