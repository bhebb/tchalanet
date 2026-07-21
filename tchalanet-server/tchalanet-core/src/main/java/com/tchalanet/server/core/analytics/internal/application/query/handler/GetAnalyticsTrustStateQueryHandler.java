package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustStateView;
import com.tchalanet.server.core.analytics.api.query.GetAnalyticsTrustStateQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Reads projection coverage for a requested business scope.
 *
 * <p>This first trust evaluator is intentionally conservative: no row for a requested business date
 * is {@code UNAVAILABLE}, not a financial zero. The reconciler adds {@code RECONCILIATION_REQUIRED}
 * once it can compare projection and transaction watermarks.
 */
@UseCase
@RequiredArgsConstructor
public class GetAnalyticsTrustStateQueryHandler
    implements QueryHandler<GetAnalyticsTrustStateQuery, AnalyticsTrustStateView> {

  private final AnalyticsDailyRepository dailyRepository;
  private final AnalyticsDrawRepository drawRepository;
  private final AnalyticsSellerTerminalDrawRepository sellerTerminalDrawRepository;
  private final Clock clock;

  @Override
  public AnalyticsTrustStateView handle(GetAnalyticsTrustStateQuery query) {
    AnalyticsTrustScope scope = query.scope();
    Set<LocalDate> availableDates = availableDates(scope);
    List<LocalDate> missingDates =
        scope
            .from()
            .datesUntil(scope.to().plusDays(1))
            .filter(date -> !availableDates.contains(date))
            .toList();
    Instant checkedAt = clock.instant();
    return missingDates.isEmpty()
        ? AnalyticsTrustStateView.ready(scope, checkedAt)
        : AnalyticsTrustStateView.unavailable(scope, missingDates, checkedAt);
  }

  private Set<LocalDate> availableDates(AnalyticsTrustScope scope) {
    return switch (scope.type()) {
      case PLATFORM -> dailyDates(dailyRepository.findPlatformRows(scope.from(), scope.to()));
      case TENANT ->
          dailyDates(
              dailyRepository.findTenantRows(scope.tenantId().value(), scope.from(), scope.to()));
      case SELLER_TERMINAL ->
          dailyDates(
              dailyRepository.findSellerTerminalRows(
                  scope.tenantId().value(),
                  scope.sellerTerminalId().value(),
                  scope.from(),
                  scope.to()));
      case DRAW ->
          drawDates(
              drawRepository.findByTenantIdAndDrawIdAndRefDateBetweenOrderByRefDate(
                  scope.tenantId().value(), scope.drawId().value(), scope.from(), scope.to()));
      case SELLER_TERMINAL_DRAW ->
          sellerTerminalDrawDates(
              sellerTerminalDrawRepository.findByTenantIdAndSellerTerminalIdAndDrawIdAndRefDate(
                  scope.tenantId().value(),
                  scope.sellerTerminalId().value(),
                  scope.drawId().value(),
                  scope.from()));
    };
  }

  private static Set<LocalDate> dailyDates(List<AnalyticsDailyEntity> rows) {
    return rows.stream()
        .map(AnalyticsDailyEntity::getRefDate)
        .collect(java.util.stream.Collectors.toSet());
  }

  private static Set<LocalDate> drawDates(List<AnalyticsDrawEntity> rows) {
    return rows.stream()
        .map(AnalyticsDrawEntity::getRefDate)
        .collect(java.util.stream.Collectors.toSet());
  }

  private static Set<LocalDate> sellerTerminalDrawDates(
      List<AnalyticsSellerTerminalDrawEntity> rows) {
    return rows.stream()
        .map(AnalyticsSellerTerminalDrawEntity::getRefDate)
        .collect(java.util.stream.Collectors.toSet());
  }
}
