package com.tchalanet.server.features.private_dashboard.block;

import java.math.BigDecimal;
import java.util.UUID;

public record OutletSummaryDto(
    UUID outletId,
    String outletCode,
    String outletName,
    String address,
    BigDecimal totalSalesToday,
    BigDecimal totalPayoutToday) {
  public static OutletSummaryDto empty() {
    return new OutletSummaryDto(null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO);
  }
}
