package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustStateView;
import java.util.List;

public record SellerTerminalDailyStatsResponse(
    boolean available,
    String trustState,
    String trustReasonCode,
    long ticketCount,
    long salesTotalCents,
    long sellerCommissionTotalCents,
    String currency,
    List<DrawStatLineItem> breakdown) {

  public static SellerTerminalDailyStatsResponse unavailable(
      String currency, AnalyticsTrustStateView trust) {
    return new SellerTerminalDailyStatsResponse(
        false,
        trust == null || trust.state() == null ? "UNAVAILABLE" : trust.state().name(),
        trust == null || trust.reasonCode() == null
            ? "analytics.trust.unknown"
            : trust.reasonCode(),
        0L,
        0L,
        0L,
        currency,
        List.of());
  }
}
