package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketJpaRepository extends TchJpaRepository<TicketJpaEntity, UUID> {
  @EntityGraph(attributePaths = "lines")
  Optional<TicketJpaEntity> findWithLinesById(UUID id);

  @EntityGraph(attributePaths = "lines")
  Optional<TicketJpaEntity> findWithLinesByTicketCode(String ticketCode);

  @EntityGraph(attributePaths = "lines")
  Optional<TicketJpaEntity> findWithLinesByPublicCode(String publicCode);

  @EntityGraph(attributePaths = "lines")
  Optional<TicketJpaEntity> findWithLinesByVerificationCode(String verificationCode);

  @EntityGraph(attributePaths = "lines")
  List<TicketJpaEntity> findWithLinesByDrawId(UUID drawId);

  @EntityGraph(attributePaths = "lines")
  List<TicketJpaEntity> findByDrawIdAndResultStatus(
      UUID drawId, TicketResultStatus resultStatus, Pageable pageable);

  long countByDrawIdAndResultStatus(UUID drawId, TicketResultStatus resultStatus);

  @EntityGraph(attributePaths = "lines")
  @Query(
      """
      SELECT t FROM TicketJpaEntity t
       WHERE t.tenantId = :tenantId
         AND t.soldAt >= :from
         AND t.soldAt < :to
      """)
  List<TicketJpaEntity> findForAnalyticsByTenantAndSoldAtRange(
      @Param("tenantId") UUID tenantId, @Param("from") Instant from, @Param("to") Instant to);

  @Query(
      """
      SELECT t FROM TicketJpaEntity t
       WHERE t.tenantId = :tenantId
         AND (:sellerTerminalId IS NULL OR t.sellerTerminalId = :sellerTerminalId)
         AND (
              (t.saleStatus = :approvedStatus)
              OR t.cancelledAt IS NOT NULL
              OR t.voidedAt IS NOT NULL
              OR t.resultedAt IS NOT NULL
              OR t.settledAt IS NOT NULL
              OR t.paidAt IS NOT NULL
         )
         AND (
              (t.soldAt >= :from AND t.soldAt < :to)
              OR (t.cancelledAt >= :from AND t.cancelledAt < :to)
              OR (t.voidedAt >= :from AND t.voidedAt < :to)
              OR (t.resultedAt >= :from AND t.resultedAt < :to)
              OR (t.settledAt >= :from AND t.settledAt < :to)
              OR (t.paidAt >= :from AND t.paidAt < :to)
              OR (t.paidAmountAdjustedAt >= :from AND t.paidAmountAdjustedAt < :to)
              OR (t.drawDate >= :fromDate AND t.drawDate <= :toDate)
         )
      """)
  List<TicketJpaEntity> findForAnalyticsActivityByTenantAndPeriod(
      @Param("tenantId") UUID tenantId,
      @Param("sellerTerminalId") UUID sellerTerminalId,
      @Param("approvedStatus") TicketSaleStatus approvedStatus,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("fromDate") java.time.LocalDate fromDate,
      @Param("toDate") java.time.LocalDate toDate);

  @EntityGraph(attributePaths = "charges")
  Optional<TicketJpaEntity> findWithChargesById(UUID id);

  @EntityGraph(attributePaths = "charges")
  List<TicketJpaEntity> findWithChargesByIdIn(List<UUID> ids);

  boolean existsByDrawIdAndSettlementStatusIn(UUID drawId, List<TicketSettlementStatus> statuses);

  long countByDrawIdAndSettlementStatusIn(UUID drawId, List<TicketSettlementStatus> statuses);

  /**
   * Returns just the @Version of an existing ticket. Kept as a narrow persistence diagnostic API;
   * updates must mutate managed entities instead of transplanting versions onto rebuilt detached
   * graphs.
   */
  @Query("select t.version from TicketJpaEntity t where t.id = :id")
  Optional<Long> findVersionById(@Param("id") UUID id);

  @Query(
      value =
          """
        SELECT ranked.rn,
               ranked.display_selection,
               ranked.game_code,
               ranked.bet_type,
               ranked.bet_option,
               ranked.line_count,
               ranked.total_stake
        FROM (
            SELECT tl.display_selection,
                   CAST(tl.game_code AS text) AS game_code,
                   CAST(tl.bet_type AS text) AS bet_type,
                   tl.bet_option,
                   COUNT(*) AS line_count,
                   COALESCE(SUM(tl.stake_amount), 0) AS total_stake,
                   ROW_NUMBER() OVER (
                       ORDER BY COUNT(*) DESC, COALESCE(SUM(tl.stake_amount), 0) DESC
                   ) AS rn
            FROM sales_ticket_line tl
            JOIN sales_ticket t ON t.id = tl.ticket_id AND t.deleted_at IS NULL
            WHERE t.tenant_id = :tenantId
              AND t.sale_status = 'APPROVED'
              AND t.sold_at >= :from
              AND t.sold_at < :to
              AND tl.deleted_at IS NULL
            GROUP BY tl.selection_key, tl.display_selection, tl.game_code, tl.bet_type, tl.bet_option
        ) ranked
        WHERE ranked.rn <= :limit
        ORDER BY ranked.rn
        """,
      nativeQuery = true)
  List<Object[]> topSelectionsByTenantAndPeriod(
      @Param("tenantId") UUID tenantId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("limit") int limit);
}
