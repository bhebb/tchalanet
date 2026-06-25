package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.core.analytics.api.model.AnalyticsDimensionType;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketPayoutReversedEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementCreatedEvent;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementReversedEvent;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Applies incremental deltas to {@code analytics_daily} rows.
 *
 * <p>Business decisions (which dimensions to update, which event fields to read)
 * live here in Java — the SQL function is a pure atomic increment primitive.
 *
 * <p>Callers (AnalyticsEventListener) ensure idempotence via ProcessedEventPort
 * before invoking any method here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsDailyProjector {

    private final AnalyticsDailyRepository repo;

    // ── ticket placed ─────────────────────────────────────────────────────────

    /**
     * Apply delta for a placed ticket.
     *
     * <p>Per the TicketPlacedEvent javadoc: only {@code APPROVED} tickets count
     * as official sales. {@code PENDING_APPROVAL} tickets are counted when they
     * transition to APPROVED via TicketApprovedEvent.
     */
    public void applyTicketPlaced(TicketPlacedEvent event, LocalDate refDate) {
        if (event.saleStatus() != TicketSaleStatus.APPROVED) {
            // PENDING_APPROVAL: wait for TicketApprovedEvent — do not count yet
            log.debug("analytics: skip PENDING ticket {}", event.ticketId().value());
            return;
        }

        long stakeCents = toCents(event.money().stake().amount());
        long sellerCommissionCents = toCents(event.context().sellerCommissionAmount());
        var charges = ChargeTotals.from(event);
        var promotions = PromotionTotals.from(event);
        UUID tenantId = event.tenantId().value();

        // Platform rollup
        upsert(AnalyticsDimensionType.PLATFORM, null, null, refDate,
            1, 0, stakeCents, stakeCents, 0, 0, sellerCommissionCents, charges, promotions, 0, 0);

        // Tenant
        upsert(AnalyticsDimensionType.TENANT, null, tenantId, refDate,
            1, 0, stakeCents, stakeCents, 0, 0, sellerCommissionCents, charges, promotions, 0, 0);

        if (event.context().sellerTerminalId() != null) {
            upsert(AnalyticsDimensionType.SELLER_TERMINAL,
                event.context().sellerTerminalId().value(),
                tenantId,
                refDate,
                1, 0, stakeCents, stakeCents, 0, 0, sellerCommissionCents, charges, promotions, 0, 0);
        }
    }

    // ── ticket cancelled ──────────────────────────────────────────────────────

    public void applyTicketCancelled(TicketCancelledEvent event, LocalDate refDate) {
        // TicketCancelledEvent does not carry the original stake amount.
        // We decrement the sold count and increment the cancelled count.
        // Gross sales reversal requires a recompute from source-of-truth data
        // (migrate-feature-stats-to-core-analytics TODO).
        UUID tenantId = event.tenantId().value();

        upsert(AnalyticsDimensionType.PLATFORM, null, null, refDate,
            -1, 1, 0, 0, 0, 0, 0, ChargeTotals.ZERO, PromotionTotals.ZERO, 0, 0);
        upsert(AnalyticsDimensionType.TENANT, null, tenantId, refDate,
            -1, 1, 0, 0, 0, 0, 0, ChargeTotals.ZERO, PromotionTotals.ZERO, 0, 0);
    }

    // ── winning settlement created ────────────────────────────────────────────

    public void applyTicketWinningSettlementCreated(
        TicketWinningSettlementCreatedEvent event,
        LocalDate refDate) {

        applyWinningsCalculatedDelta(event.tenantId().value(), refDate, event.amountCents());
    }

    public void applyTicketWinningSettlementReversed(
        TicketWinningSettlementReversedEvent event,
        LocalDate refDate) {

        applyWinningsCalculatedDelta(event.tenantId().value(), refDate, -event.amountCents());
    }

    private void applyWinningsCalculatedDelta(UUID tenantId, LocalDate refDate, long winningsCentsDelta) {
        upsert(AnalyticsDimensionType.PLATFORM, null, null, refDate,
            0, 0, 0, 0, winningsCentsDelta, 0, 0, ChargeTotals.ZERO, PromotionTotals.ZERO, 0, 0);
        upsert(AnalyticsDimensionType.TENANT, null, tenantId, refDate,
            0, 0, 0, 0, winningsCentsDelta, 0, 0, ChargeTotals.ZERO, PromotionTotals.ZERO, 0, 0);
    }

    // ── payout paid / reversed ────────────────────────────────────────────────

    public void applyTicketPayoutPaid(TicketPayoutPaidEvent event, LocalDate refDate) {
        applyPayoutPaidDelta(event.tenantId().value(), refDate, event.amountCents());
    }

    public void applyTicketPayoutReversed(TicketPayoutReversedEvent event, LocalDate refDate) {
        applyPayoutPaidDelta(event.tenantId().value(), refDate, -event.amountCents());
    }

    private void applyPayoutPaidDelta(UUID tenantId, LocalDate refDate, long paidCentsDelta) {
        upsert(AnalyticsDimensionType.PLATFORM, null, null, refDate,
            0, 0, 0, 0, 0, paidCentsDelta, 0, ChargeTotals.ZERO, PromotionTotals.ZERO, 0, 0);
        upsert(AnalyticsDimensionType.TENANT, null, tenantId, refDate,
            0, 0, 0, 0, 0, paidCentsDelta, 0, ChargeTotals.ZERO, PromotionTotals.ZERO, 0, 0);
    }

    // ── session opened ───────────────────────────────────────────────────────


    // ── helper ────────────────────────────────────────────────────────────────

    private void upsert(
        AnalyticsDimensionType dimensionType,
        UUID dimensionId,
        UUID tenantId,
        LocalDate refDate,
        long ticketsSoldDelta,
        long ticketsCancelledDelta,
        long grossSalesDelta,
        long stakeTotalDelta,
        long winningsCalcDelta,
        long payoutsPaidDelta,
        long sellerCommissionDelta,
        ChargeTotals charges,
        PromotionTotals promotions,
        long sessionsOpenedDelta,
        long sessionsClosedDelta) {

        repo.upsertAndIncrement(
            dimensionType.name(), dimensionId, tenantId, refDate,
            ticketsSoldDelta, ticketsCancelledDelta,
            grossSalesDelta, stakeTotalDelta,
            winningsCalcDelta, payoutsPaidDelta,
            sellerCommissionDelta,
            charges.buyerCents(),
            charges.sellerCents(),
            charges.tenantCents(),
            charges.waivedCents(),
            promotions.lineCount(),
            promotions.pricedLineCount(),
            promotions.payoutBaseCents(),
            promotions.potentialPayoutCents(),
            sessionsOpenedDelta, sessionsClosedDelta);
    }

    private static long toCents(BigDecimal amount) {
        return amount == null ? 0L : amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private record ChargeTotals(long buyerCents, long sellerCents, long tenantCents, long waivedCents) {
        static final ChargeTotals ZERO = new ChargeTotals(0, 0, 0, 0);

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

    private record PromotionTotals(
        long lineCount,
        long pricedLineCount,
        long payoutBaseCents,
        long potentialPayoutCents
    ) {
        static final PromotionTotals ZERO = new PromotionTotals(0, 0, 0, 0);

        static PromotionTotals from(TicketPlacedEvent event) {
            long lineCount = 0L;
            long pricedLineCount = 0L;
            long payoutBase = 0L;
            long potential = 0L;

            for (var line : event.lines()) {
                if (line.origin() == TicketLineOrigin.PROMOTION) {
                    lineCount++;
                    payoutBase += toCents(line.payoutBaseAmount() != null ? line.payoutBaseAmount().amount() : null);
                    potential += toCents(
                        line.potentialPayoutAmount() != null ? line.potentialPayoutAmount().amount() : null);
                }
                if (line.pricingSource() == TicketLinePricingSource.PROMOTION) {
                    pricedLineCount++;
                }
            }

            return new PromotionTotals(lineCount, pricedLineCount, payoutBase, potential);
        }
    }
}
