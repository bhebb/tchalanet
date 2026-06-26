package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutReversedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementCreatedEvent;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementReversedEvent;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ensures an {@code analytics_draw} row exists for every resulted draw.
 *
 * <p>The row is created with zero counters; downstream ticket-settlement events
 * (TicketWinningSettlementCreatedEvent) updates the winnings columns via the daily projector.
 * The draw row exists primarily to serve per-draw breakdown queries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsDrawProjector {

  private final AnalyticsDrawRepository repo;
  private final Clock clock;

  public void applyTicketPlaced(TicketPlacedEvent event, LocalDate refDate) {
    if (event.saleStatus() != TicketSaleStatus.APPROVED) {
      log.debug("analytics-draw: skip PENDING ticket {}", event.ticketId().value());
      return;
    }

    UUID drawId = event.context().drawId().value();
    Instant now = Instant.now(clock);
    long stakeCents = toCents(event.money().stake().amount());
    long sellerCommissionCents = toCents(event.context().sellerCommissionAmount());
    var charges = ChargeTotals.from(event);
    var promotions = PromotionTotals.from(event);

    AnalyticsDrawEntity entity = repo.findByDrawId(drawId)
        .orElseGet(() -> AnalyticsDrawEntity.builder()
            .drawId(drawId)
            .tenantId(event.tenantId().value())
            .gameCode(gameCodeFor(event))
            .drawChannelCode(event.context().drawChannelId() != null
                ? event.context().drawChannelId().value().toString() : null)
            .scheduledAt(event.occurredAt())
            .refDate(refDate)
            .ticketsSoldCount(0L)
            .ticketsCancelledCount(0L)
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
    entity.setUpdatedAt(now);

    repo.save(entity);
  }

  public void applyTicketWinningSettlementCreated(TicketWinningSettlementCreatedEvent event, LocalDate refDate) {
    applyWinningsCalculatedDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        event.amountCents());
  }

  public void applyTicketWinningSettlementReversed(TicketWinningSettlementReversedEvent event, LocalDate refDate) {
    applyWinningsCalculatedDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        -event.amountCents());
  }

  public void applyTicketPayoutPaid(TicketPayoutPaidEvent event, LocalDate refDate) {
    applyPayoutPaidDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        event.amountCents());
  }

  public void applyTicketPayoutReversed(TicketPayoutReversedEvent event, LocalDate refDate) {
    applyPayoutPaidDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        -event.amountCents());
  }

  /** Ensure a draw row exists and enrich metadata known when the result is applied. */
  public void ensureDrawRow(DrawResultAppliedEvent event) {
    UUID drawId = event.drawId().value();
    Instant now = Instant.now(clock);
    AnalyticsDrawEntity entity = repo.findByDrawId(drawId)
        .orElseGet(() -> AnalyticsDrawEntity.builder()
            .drawId(drawId)
            .ticketsSoldCount(0L)
            .ticketsCancelledCount(0L)
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
            .build());

    entity.setTenantId(event.tenantId().value());
    if (entity.getGameCode() == null) {
      entity.setGameCode("UNKNOWN"); // enriched by recompute or TicketPlaced aggregation
    }
    entity.setDrawChannelCode(event.drawChannelId() != null ? event.drawChannelId().value().toString() : null);
    entity.setScheduledAt(event.occurredAt());
    entity.setRefDate(event.drawDate());
    entity.setUpdatedAt(now);

    repo.save(entity);
    log.debug("analytics_draw row ensured for draw {}", drawId);
  }

  private void applyWinningsCalculatedDelta(
      UUID drawId,
      UUID tenantId,
      LocalDate refDate,
      Instant occurredAt,
      long winningsCentsDelta) {

    var entity = ensureFinancialRow(drawId, tenantId, refDate, occurredAt);
    entity.setWinningsCalculatedCents(entity.getWinningsCalculatedCents() + winningsCentsDelta);
    entity.setNetRevenueEstimatedCents(entity.getNetRevenueEstimatedCents() - winningsCentsDelta);
    entity.setUpdatedAt(Instant.now(clock));
    repo.save(entity);
  }

  private void applyPayoutPaidDelta(
      UUID drawId,
      UUID tenantId,
      LocalDate refDate,
      Instant occurredAt,
      long paidCentsDelta) {

    var entity = ensureFinancialRow(drawId, tenantId, refDate, occurredAt);
    entity.setPayoutsPaidCents(entity.getPayoutsPaidCents() + paidCentsDelta);
    entity.setNetRevenuePaidBasisCents(entity.getNetRevenuePaidBasisCents() - paidCentsDelta);
    entity.setUpdatedAt(Instant.now(clock));
    repo.save(entity);
  }

  private AnalyticsDrawEntity ensureFinancialRow(
      UUID drawId,
      UUID tenantId,
      LocalDate refDate,
      Instant occurredAt) {

    Instant now = Instant.now(clock);
    return repo.findByDrawId(drawId)
        .orElseGet(() -> AnalyticsDrawEntity.builder()
            .drawId(drawId)
            .tenantId(tenantId)
            .gameCode("UNKNOWN")
            .scheduledAt(occurredAt)
            .refDate(refDate)
            .ticketsSoldCount(0L)
            .ticketsCancelledCount(0L)
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

  record ChargeTotals(long buyerCents, long sellerCents, long tenantCents, long waivedCents) {
    static ChargeTotals from(TicketPlacedEvent event) {
      long buyer = 0L;
      long seller = 0L;
      long tenant = 0L;
      long waived = 0L;

      for (var charge : event.money().charges()) {
        long amount = toCents(charge.amount() != null ? charge.amount().amount() : null);
        if (charge.waived()) {
          waived += amount;
          continue;
        }

        if (charge.paidBy() == ChargePaidBy.BUYER) {
          buyer += amount;
        } else if (charge.paidBy() == ChargePaidBy.SELLER) {
          seller += amount;
        } else if (charge.paidBy() == ChargePaidBy.TENANT) {
          tenant += amount;
        }
      }

      return new ChargeTotals(buyer, seller, tenant, waived);
    }
  }

  record PromotionTotals(
      long lineCount,
      long pricedLineCount,
      long payoutBaseCents,
      long potentialPayoutCents
  ) {
    static PromotionTotals from(TicketPlacedEvent event) {
      long lineCount = 0L;
      long pricedLineCount = 0L;
      long payoutBase = 0L;
      long potential = 0L;

      for (var line : event.lines()) {
        if (line.origin() == TicketLineOrigin.PROMOTION) {
          lineCount++;
          payoutBase += toCents(line.payoutBaseAmount() != null ? line.payoutBaseAmount().amount() : null);
          potential += toCents(line.potentialPayoutAmount() != null
              ? line.potentialPayoutAmount().amount() : null);
        }
        if (line.pricingSource() == TicketLinePricingSource.PROMOTION) {
          pricedLineCount++;
        }
      }

      return new PromotionTotals(lineCount, pricedLineCount, payoutBase, potential);
    }
  }
}
