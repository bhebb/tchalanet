package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketChargeSnapshot;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketLineSnapshot;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsTicketSnapshotsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.SalesAnalyticsSnapshotReaderPort;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketChargeJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketLineJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketChargeJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of the sales-to-analytics reconciliation read boundary. */
@Component
@RequiredArgsConstructor
class SalesAnalyticsSnapshotReaderAdapter implements SalesAnalyticsSnapshotReaderPort {

  private final TicketJpaRepository ticketRepository;
  private final TicketChargeJpaRepository chargeRepository;

  @Override
  @Transactional(readOnly = true)
  public List<SalesAnalyticsTicketSnapshot> findTicketSnapshots(
      GetSalesAnalyticsTicketSnapshotsQuery query) {
    var tickets =
        ticketRepository
            .findForAnalyticsActivityByTenantAndPeriod(
                query.tenantId().value(),
                null,
                TicketSaleStatus.APPROVED,
                query.from(),
                query.to())
            .stream()
            .toList();
    if (tickets.isEmpty()) {
      return List.of();
    }

    var chargesByTicketId =
        chargeRepository
            .findByTicket_IdInOrderByTicket_IdAscChargeTypeAsc(
                tickets.stream().map(TicketJpaEntity::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(charge -> charge.getTicket().getId()));

    return tickets.stream()
        .map(
            ticket -> toSnapshot(ticket, chargesByTicketId.getOrDefault(ticket.getId(), List.of())))
        .toList();
  }

  private SalesAnalyticsTicketSnapshot toSnapshot(
      TicketJpaEntity ticket, List<TicketChargeJpaEntity> charges) {
    return new SalesAnalyticsTicketSnapshot(
        ticket.getId(),
        ticket.getTenantId(),
        ticket.getSellerTerminalId(),
        ticket.getDrawId(),
        ticket.getDrawChannelId(),
        ticket.getSoldAt(),
        ticket.getDrawScheduledAt(),
        ticket.getSaleStatus().name(),
        ticket.getCancelledAt(),
        ticket.getVoidedAt(),
        ticket.getResultStatus().name(),
        ticket.getResultedAt(),
        ticket.getSettlementStatus().name(),
        ticket.getSettledAt(),
        ticket.getPaidAt(),
        ticket.getPaidAmount(),
        ticket.getPaidAmountAdjustedAt(),
        ticket.getPaidAmountAdjustedBy(),
        ticket.getPaidAmountAdjustmentReason(),
        ticket.getStakeAmount(),
        ticket.getWinningAmount(),
        ticket.getSellerCommissionAmountSnapshot(),
        ticket.getLines().stream().map(this::toLineSnapshot).toList(),
        charges.stream().map(this::toChargeSnapshot).toList());
  }

  private SalesAnalyticsTicketLineSnapshot toLineSnapshot(TicketLineJpaEntity line) {
    return new SalesAnalyticsTicketLineSnapshot(
        line.getGameCode().name(),
        line.getBetType().name(),
        line.getBetOption(),
        line.getSelectionKey(),
        line.getStakeAmount(),
        line.getPayoutAmount(),
        line.getOrigin().name(),
        line.getPricingSource().name());
  }

  private SalesAnalyticsTicketChargeSnapshot toChargeSnapshot(TicketChargeJpaEntity charge) {
    return new SalesAnalyticsTicketChargeSnapshot(
        charge.getPaidBy().name(),
        charge.getAmount(),
        charge.getWaivedByDecisionId() != null || charge.getWaivedByRuleId() != null);
  }
}
