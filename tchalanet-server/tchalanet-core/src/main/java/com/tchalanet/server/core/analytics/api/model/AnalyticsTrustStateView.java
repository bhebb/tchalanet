package com.tchalanet.server.core.analytics.api.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Result of checking whether a requested analytics scope can safely render financial metrics. */
public record AnalyticsTrustStateView(
    AnalyticsTrustState state,
    AnalyticsTrustScope scope,
    List<LocalDate> missingBusinessDates,
    String reasonCode,
    Instant checkedAt) {

  public static AnalyticsTrustStateView ready(AnalyticsTrustScope scope, Instant checkedAt) {
    return new AnalyticsTrustStateView(
        AnalyticsTrustState.READY, scope, List.of(), "analytics.trust.ready", checkedAt);
  }

  public static AnalyticsTrustStateView unavailable(
      AnalyticsTrustScope scope, List<LocalDate> missingBusinessDates, Instant checkedAt) {
    return new AnalyticsTrustStateView(
        AnalyticsTrustState.UNAVAILABLE,
        scope,
        List.copyOf(missingBusinessDates),
        "analytics.trust.projection_missing",
        checkedAt);
  }
}
