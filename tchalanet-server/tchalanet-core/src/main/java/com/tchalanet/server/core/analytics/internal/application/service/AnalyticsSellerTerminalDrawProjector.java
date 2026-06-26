package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutReversedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementCreatedEvent;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementReversedEvent;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Projects exact financial rows by seller terminal and draw. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSellerTerminalDrawProjector {

  private final AnalyticsSellerTerminalDrawRepository repository;
  private final Clock clock;

  public void applyTicketPlaced(TicketPlacedEvent event, LocalDate refDate) {
    if (event.saleStatus() != TicketSaleStatus.APPROVED) {
      log.debug("analytics-seller-terminal-draw: skip PENDING ticket {}", event.ticketId().value());
      return;
    }
    if (event.context().sellerTerminalId() == null) {
      log.debug("analytics-seller-terminal-draw: skip ticket without seller terminal {}", event.ticketId().value());
      return;
    }

    long stakeCents = toCents(event.money().stake().amount());
    long sellerCommissionCents = toCents(event.context().sellerCommissionAmount());
    var charges = AnalyticsDrawProjector.ChargeTotals.from(event);
    var promotions = AnalyticsDrawProjector.PromotionTotals.from(event);
    var entity = ensureRow(
        event.tenantId().value(),
        event.context().sellerTerminalId().value(),
        event.context().drawId().value(),
        refDate,
        event.occurredAt());

    entity.setGameCode(gameCodeFor(event));
    entity.setDrawChannelCode(event.context().drawChannelId() != null
        ? event.context().drawChannelId().value().toString() : null);
    entity.setTicketsSoldCount(entity.getTicketsSoldCount() + 1);
    entity.setGrossSalesCents(entity.getGrossSalesCents() + stakeCents);
    entity.setStakeTotalCents(entity.getStakeTotalCents() + stakeCents);
    entity.setSellerCommissionCents(entity.getSellerCommissionCents() + sellerCommissionCents);
    entity.setBuyerChargeCents(entity.getBuyerChargeCents() + charges.buyerCents());
    entity.setSellerChargeCents(entity.getSellerChargeCents() + charges.sellerCents());
    entity.setTenantChargeCents(entity.getTenantChargeCents() + charges.tenantCents());
    entity.setWaivedChargeCents(entity.getWaivedChargeCents() + charges.waivedCents());
    entity.setPromotionLineCount(entity.getPromotionLineCount() + promotions.lineCount());
    entity.setPromotionPricedLineCount(entity.getPromotionPricedLineCount() + promotions.pricedLineCount());
    entity.setPromotionPayoutBaseCents(entity.getPromotionPayoutBaseCents() + promotions.payoutBaseCents());
    entity.setPromotionPotentialPayoutCents(
        entity.getPromotionPotentialPayoutCents() + promotions.potentialPayoutCents());
    entity.setNetRevenueEstimatedCents(
        entity.getNetRevenueEstimatedCents() + stakeCents - sellerCommissionCents - charges.tenantCents());
    entity.setNetRevenuePaidBasisCents(
        entity.getNetRevenuePaidBasisCents() + stakeCents - sellerCommissionCents - charges.tenantCents());
    entity.setUpdatedAt(Instant.now(clock));
    repository.save(entity);
  }

  public void applyTicketWinningSettlementCreated(TicketWinningSettlementCreatedEvent event, LocalDate refDate) {
    if (event.sellerTerminalId() == null) {
      return;
    }
    applyWinningsDelta(
        event.tenantId().value(),
        event.sellerTerminalId().value(),
        event.drawId().value(),
        refDate,
        event.occurredAt(),
        event.amountCents());
  }

  public void applyTicketWinningSettlementReversed(TicketWinningSettlementReversedEvent event, LocalDate refDate) {
    if (event.sellerTerminalId() == null) {
      return;
    }
    applyWinningsDelta(
        event.tenantId().value(),
        event.sellerTerminalId().value(),
        event.drawId().value(),
        refDate,
        event.occurredAt(),
        -event.amountCents());
  }

  public void applyTicketPayoutPaid(TicketPayoutPaidEvent event, LocalDate refDate) {
    if (event.sellerTerminalId() == null) {
      return;
    }
    applyPayoutDelta(
        event.tenantId().value(),
        event.sellerTerminalId().value(),
        event.drawId().value(),
        refDate,
        event.occurredAt(),
        event.amountCents());
  }

  public void applyTicketPayoutReversed(TicketPayoutReversedEvent event, LocalDate refDate) {
    if (event.sellerTerminalId() == null) {
      return;
    }
    applyPayoutDelta(
        event.tenantId().value(),
        event.sellerTerminalId().value(),
        event.drawId().value(),
        refDate,
        event.occurredAt(),
        -event.amountCents());
  }

  private void applyWinningsDelta(
      UUID tenantId,
      UUID sellerTerminalId,
      UUID drawId,
      LocalDate refDate,
      Instant occurredAt,
      long amountCentsDelta) {

    var entity = ensureRow(tenantId, sellerTerminalId, drawId, refDate, occurredAt);
    entity.setWinningsCalculatedCents(entity.getWinningsCalculatedCents() + amountCentsDelta);
    entity.setNetRevenueEstimatedCents(entity.getNetRevenueEstimatedCents() - amountCentsDelta);
    entity.setUpdatedAt(Instant.now(clock));
    repository.save(entity);
  }

  private void applyPayoutDelta(
      UUID tenantId,
      UUID sellerTerminalId,
      UUID drawId,
      LocalDate refDate,
      Instant occurredAt,
      long amountCentsDelta) {

    var entity = ensureRow(tenantId, sellerTerminalId, drawId, refDate, occurredAt);
    entity.setPayoutsPaidCents(entity.getPayoutsPaidCents() + amountCentsDelta);
    entity.setNetRevenuePaidBasisCents(entity.getNetRevenuePaidBasisCents() - amountCentsDelta);
    entity.setUpdatedAt(Instant.now(clock));
    repository.save(entity);
  }

  private AnalyticsSellerTerminalDrawEntity ensureRow(
      UUID tenantId,
      UUID sellerTerminalId,
      UUID drawId,
      LocalDate refDate,
      Instant occurredAt) {

    Instant now = Instant.now(clock);
    return repository.findByTenantIdAndSellerTerminalIdAndDrawId(tenantId, sellerTerminalId, drawId)
        .orElseGet(() -> AnalyticsSellerTerminalDrawEntity.builder()
            .tenantId(tenantId)
            .sellerTerminalId(sellerTerminalId)
            .drawId(drawId)
            .refDate(refDate)
            .scheduledAt(occurredAt)
            .gameCode("UNKNOWN")
            .ticketsSoldCount(0L)
            .grossSalesCents(0L)
            .stakeTotalCents(0L)
            .winningsCalculatedCents(0L)
            .payoutsPaidCents(0L)
            .sellerCommissionCents(0L)
            .buyerChargeCents(0L)
            .sellerChargeCents(0L)
            .tenantChargeCents(0L)
            .waivedChargeCents(0L)
            .promotionLineCount(0L)
            .promotionPricedLineCount(0L)
            .promotionPayoutBaseCents(0L)
            .promotionPotentialPayoutCents(0L)
            .netRevenueEstimatedCents(0L)
            .netRevenuePaidBasisCents(0L)
            .createdAt(now)
            .updatedAt(now)
            .build());
  }

  private static long toCents(BigDecimal amount) {
    return amount == null ? 0L : amount.multiply(BigDecimal.valueOf(100)).longValue();
  }

  private static String gameCodeFor(TicketPlacedEvent event) {
    var gameCodes = event.lines().stream()
        .map(line -> line.gameCode().name())
        .collect(Collectors.toSet());
    if (gameCodes.size() == 1) {
      return gameCodes.iterator().next();
    }
    return "MIXED";
  }
}
