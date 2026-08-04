package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidAmountAdjustedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutReversedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the {@code analytics_draw} row that backs the per-draw financial breakdown.
 *
 * <p>The row is created by the first sale on a draw, and only by a sale. Result application then
 * enriches it — it no longer seeds a row of its own, because a draw resulted without any sale has
 * no financial story to tell and those empty rows were the bulk of what the reports displayed.
 *
 * <p>Downstream payout-paid events update both winnings and payouts, because result application
 * auto-settles winning tickets.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsDrawProjector {

  private final AnalyticsDrawRepository repo;
  private final Clock clock;

  @Transactional
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

    AnalyticsDrawEntity entity =
        repo.findByDrawId(drawId)
            .orElseGet(
                () ->
                    AnalyticsDrawEntity.builder()
                        .drawId(drawId)
                        .tenantId(event.tenantId().value())
                        .gameCode(gameCodeFor(event))
                        .drawChannelCode(
                            event.context().drawChannelId() != null
                                ? event.context().drawChannelId().value().toString()
                                : null)
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
    entity.setPromotionPricedLineCount(
        entity.getPromotionPricedLineCount() + promotions.pricedLineCount());
    entity.setNetRevenueEstimatedCents(
        entity.getNetRevenueEstimatedCents()
            + stakeCents
            - sellerCommissionCents
            - charges.tenantCents());
    entity.setNetRevenuePaidBasisCents(
        entity.getNetRevenuePaidBasisCents()
            + stakeCents
            - sellerCommissionCents
            - charges.tenantCents());
    entity.setUpdatedAt(now);

    repo.save(entity);
  }

  @Transactional
  public void applyTicketSettledAndPaid(TicketPayoutPaidEvent event, LocalDate refDate) {
    applyWinningsCalculatedDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        event.amountCents());
    applyPayoutPaidDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        event.amountCents());
  }

  @Transactional
  public void applyTicketSettlementAndPayoutReversed(
      TicketPayoutReversedEvent event, LocalDate refDate) {
    applyWinningsCalculatedDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        -event.amountCents());
    applyPayoutPaidDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        -event.amountCents());
  }

  @Transactional
  public void applyTicketPayoutPaid(TicketPayoutPaidEvent event, LocalDate refDate) {
    applyPayoutPaidDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        event.amountCents());
  }

  @Transactional
  public void applyTicketPayoutPaidAmountAdjusted(
      TicketPayoutPaidAmountAdjustedEvent event, LocalDate refDate) {
    applyPayoutPaidDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        event.deltaAmountCents());
  }

  @Transactional
  public void applyTicketPayoutReversed(TicketPayoutReversedEvent event, LocalDate refDate) {
    applyPayoutPaidDelta(
        event.drawId().value(),
        event.tenantId().value(),
        refDate,
        event.occurredAt(),
        -event.amountCents());
  }

  /** Reverses the approved sale contribution when the ticket is cancelled before result. */
  @Transactional
  public void applyTicketCancelled(SalesAnalyticsTicketSnapshot snapshot) {
    if (snapshot.approvedAt() == null || snapshot.drawId() == null) {
      return;
    }
    var entity = repo.findByDrawId(snapshot.drawId()).orElse(null);
    if (entity == null) {
      return;
    }

    long stakeCents = toCents(snapshot.stakeAmount());
    long commissionCents = toCents(snapshot.sellerCommissionAmount());
    var charges = ChargeTotals.from(snapshot);
    var promotions = PromotionTotals.from(snapshot);

    entity.setTicketsSoldCount(entity.getTicketsSoldCount() - 1);
    entity.setTicketsCancelledCount(entity.getTicketsCancelledCount() + 1);
    entity.setGrossSalesCents(entity.getGrossSalesCents() - stakeCents);
    entity.setStakeTotalCents(entity.getStakeTotalCents() - stakeCents);
    entity.setSellerCommissionCents(entity.getSellerCommissionCents() - commissionCents);
    entity.setBuyerChargeCents(entity.getBuyerChargeCents() - charges.buyerCents());
    entity.setSellerChargeCents(entity.getSellerChargeCents() - charges.sellerCents());
    entity.setTenantChargeCents(entity.getTenantChargeCents() - charges.tenantCents());
    entity.setWaivedChargeCents(entity.getWaivedChargeCents() - charges.waivedCents());
    entity.setPromotionLineCount(entity.getPromotionLineCount() - promotions.lineCount());
    entity.setPromotionPricedLineCount(
        entity.getPromotionPricedLineCount() - promotions.pricedLineCount());
    entity.setNetRevenueEstimatedCents(
        entity.getNetRevenueEstimatedCents()
            - stakeCents
            + commissionCents
            + charges.tenantCents());
    entity.setNetRevenuePaidBasisCents(
        entity.getNetRevenuePaidBasisCents()
            - stakeCents
            + commissionCents
            + charges.tenantCents());
    entity.setUpdatedAt(Instant.now(clock));
    repo.save(entity);
  }

  /**
   * Enrich the financial row of a resulted draw with the metadata known at result time.
   *
   * <p>Deliberately does <em>not</em> create the row. A draw that was resulted without a single
   * sale has nothing financial to report, and seeding one row per resulted draw filled the reports
   * with zero-valued lines — they were all the daily sales report ever showed. The row is created
   * by the sale itself ({@link #applyTicketPlaced}); if there is none, there were no sales, and
   * there is nothing to enrich.
   */
  @Transactional
  public void ensureDrawRow(DrawResultAppliedEvent event) {
    UUID drawId = event.drawId().value();
    var existing = repo.findByDrawId(drawId);
    if (existing.isEmpty()) {
      log.debug("analytics_draw no sales for draw {} — nothing to enrich", drawId);
      return;
    }

    AnalyticsDrawEntity entity = existing.get();
    entity.setTenantId(event.tenantId().value());
    entity.setDrawChannelCode(
        event.drawChannelId() != null ? event.drawChannelId().value().toString() : null);
    entity.setScheduledAt(event.occurredAt());
    entity.setRefDate(event.drawDate());
    entity.setUpdatedAt(Instant.now(clock));

    repo.save(entity);
    log.debug("analytics_draw row enriched for draw {}", drawId);
  }

  private void applyWinningsCalculatedDelta(
      UUID drawId, UUID tenantId, LocalDate refDate, Instant occurredAt, long winningsCentsDelta) {

    var entity = ensureFinancialRow(drawId, tenantId, refDate, occurredAt);
    entity.setWinningsCalculatedCents(entity.getWinningsCalculatedCents() + winningsCentsDelta);
    entity.setNetRevenueEstimatedCents(entity.getNetRevenueEstimatedCents() - winningsCentsDelta);
    entity.setUpdatedAt(Instant.now(clock));
    repo.save(entity);
  }

  private void applyPayoutPaidDelta(
      UUID drawId, UUID tenantId, LocalDate refDate, Instant occurredAt, long paidCentsDelta) {

    var entity = ensureFinancialRow(drawId, tenantId, refDate, occurredAt);
    entity.setPayoutsPaidCents(entity.getPayoutsPaidCents() + paidCentsDelta);
    entity.setNetRevenuePaidBasisCents(entity.getNetRevenuePaidBasisCents() - paidCentsDelta);
    entity.setUpdatedAt(Instant.now(clock));
    repo.save(entity);
  }

  private AnalyticsDrawEntity ensureFinancialRow(
      UUID drawId, UUID tenantId, LocalDate refDate, Instant occurredAt) {

    Instant now = Instant.now(clock);
    return repo.findByDrawId(drawId)
        .orElseGet(
            () ->
                AnalyticsDrawEntity.builder()
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
    var gameCodes =
        event.lines().stream().map(line -> line.gameCode().name()).collect(Collectors.toSet());
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

    static ChargeTotals from(SalesAnalyticsTicketSnapshot snapshot) {
      long buyer = 0L;
      long seller = 0L;
      long tenant = 0L;
      long waived = 0L;
      for (var charge : snapshot.charges()) {
        long amount = toCents(charge.amount());
        if (charge.waived()) {
          waived += amount;
        } else if ("BUYER".equals(charge.paidBy())) {
          buyer += amount;
        } else if ("SELLER".equals(charge.paidBy())) {
          seller += amount;
        } else if ("TENANT".equals(charge.paidBy())) {
          tenant += amount;
        }
      }
      return new ChargeTotals(buyer, seller, tenant, waived);
    }
  }

  record PromotionTotals(long lineCount, long pricedLineCount, long payoutBaseCents) {
    static PromotionTotals from(TicketPlacedEvent event) {
      long lineCount = 0L;
      long pricedLineCount = 0L;

      for (var line : event.lines()) {
        if (line.origin() == TicketLineOrigin.PROMOTION) {
          lineCount++;
        }
        if (line.pricingSource() == TicketLinePricingSource.PROMOTION) {
          pricedLineCount++;
        }
      }

      return new PromotionTotals(lineCount, pricedLineCount, 0L);
    }

    static PromotionTotals from(SalesAnalyticsTicketSnapshot snapshot) {
      long lineCount =
          snapshot.lines().stream().filter(line -> "PROMOTION".equals(line.origin())).count();
      long pricedLineCount =
          snapshot.lines().stream()
              .filter(line -> "PROMOTION".equals(line.pricingSource()))
              .count();
      return new PromotionTotals(lineCount, pricedLineCount, 0L);
    }
  }
}
