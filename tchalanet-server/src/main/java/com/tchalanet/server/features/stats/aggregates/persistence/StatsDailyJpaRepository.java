package com.tchalanet.server.features.stats.aggregates.persistence;

import com.tchalanet.server.features.stats.cashierdashboard.model.CashierAggregateRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StatsDailyJpaRepository
    extends JpaRepository<StatsDailyEntity, UUID>, StatsDailyCustomRepository {

  Optional<StatsDailyEntity> findByDimensionTypeAndDimensionIdAndRefDate(
      String dimensionType, UUID dimensionId, LocalDate refDate);

  List<StatsDailyEntity> findByDimensionTypeAndDimensionIdAndRefDateBetween(
      String dimensionType, UUID dimensionId, LocalDate from, LocalDate to);

  void deleteByRefDateBetween(LocalDate from, LocalDate to);

  // Pour un caissier donné (stats du dashboard)
  @Query(
      """
        select s
        from StatsDailyEntity s
        where s.dimensionType = 'cashier'
          and s.dimensionId = :cashierId
          and s.refDate between :from and :to
        order by s.refDate asc
        """)
  List<StatsDailyEntity> findByCashierAndDateRange(UUID cashierId, LocalDate from, LocalDate to);

  // Pour top caissiers d'un tenant :
  @Query(
      """
        select new com.tchalanet.server.features.stats.cashierdashboard.app.CashierAggregateRow(
            s.dimensionId,
            sum(s.ticketsCount),
            sum(s.stakeSumCents),
            sum(s.winningsSumCents),
            sum(s.netRevenueCents)
        )
        from StatsDailyEntity s
        join AppUserJpaEntity c on c.id = s.dimensionId
        where s.dimensionType = 'cashier'
          and s.refDate between :from and :to
        group by s.dimensionId
        order by sum(s.netRevenueCents) desc
        """)
  List<CashierAggregateRow> findTopCashiersByTenantAndDateRange(
      UUID tenantId, LocalDate from, LocalDate to, int limit);
}
