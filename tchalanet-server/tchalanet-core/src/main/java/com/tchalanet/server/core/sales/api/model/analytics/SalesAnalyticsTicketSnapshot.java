package com.tchalanet.server.core.sales.api.model.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Immutable sales-side source snapshot used only to reconcile analytics projections.
 *
 * <p>It deliberately contains the values captured on the ticket and its lines, never a current
 * pricing, promotion, commission, or draw-channel configuration.
 */
public record SalesAnalyticsTicketSnapshot(
    UUID ticketId,
    UUID tenantId,
    UUID sellerTerminalId,
    UUID drawId,
    UUID drawChannelId,
    Instant soldAt,
    Instant approvedAt,
    Instant drawScheduledAt,
    LocalDate drawDate,
    String saleStatus,
    Instant cancelledAt,
    Instant voidedAt,
    String resultStatus,
    Instant resultedAt,
    String settlementStatus,
    Instant settledAt,
    Instant paidAt,
    BigDecimal paidAmount,
    Instant paidAmountAdjustedAt,
    UUID paidAmountAdjustedBy,
    String paidAmountAdjustmentReason,
    BigDecimal stakeAmount,
    BigDecimal winningAmount,
    BigDecimal sellerCommissionAmount,
    List<SalesAnalyticsTicketLineSnapshot> lines,
    List<SalesAnalyticsTicketChargeSnapshot> charges) {

  public SalesAnalyticsTicketSnapshot {
    lines = List.copyOf(lines);
    charges = List.copyOf(charges);
  }
}
