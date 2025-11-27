package com.tchalanet.server.stats.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Represents the calculated statistics for a single, completed draw. This is a data-centric object,
 * making 'record' a good choice.
 */
public record DrawStats(
    UUID drawId,
    UUID tenantId,
    long totalTickets,
    long totalLines,
    BigDecimal totalStake,
    BigDecimal totalPayout,
    long winnersCount,
    long losersCount,
    BigDecimal grossMargin,
    BigDecimal marginPct) {
  /** Factory method to calculate derived fields like margin and margin percentage. */
  public static DrawStats calculate(
      UUID drawId,
      UUID tenantId,
      long totalTickets,
      long totalLines,
      BigDecimal totalStake,
      BigDecimal totalPayout,
      long winnersCount) {
    BigDecimal grossMargin = totalStake.subtract(totalPayout);
    BigDecimal marginPct = BigDecimal.ZERO;
    if (totalStake.compareTo(BigDecimal.ZERO) > 0) {
      marginPct = grossMargin.divide(totalStake, 4, RoundingMode.HALF_UP);
    }
    long losersCount = totalTickets - winnersCount;

    return new DrawStats(
        drawId,
        tenantId,
        totalTickets,
        totalLines,
        totalStake,
        totalPayout,
        winnersCount,
        losersCount,
        grossMargin,
        marginPct);
  }
}
