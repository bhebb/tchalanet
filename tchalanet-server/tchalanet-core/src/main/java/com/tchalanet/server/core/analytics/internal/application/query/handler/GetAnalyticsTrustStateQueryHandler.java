package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScopeType;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustStateView;
import com.tchalanet.server.core.analytics.api.query.GetAnalyticsTrustStateQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsVisibilityOverrideRepository;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsActivityDatesQuery;
import com.tchalanet.server.platform.tenant.api.TenantZoneApi;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Reads projection coverage for a requested business scope.
 *
 * <p>This first trust evaluator is intentionally conservative: no row for a requested business date
 * is {@code UNAVAILABLE}, not a financial zero, when source ticket activity exists. Tenant and
 * seller-terminal scopes can distinguish an empty business date from a missing projection by
 * checking the source ticket lifecycle. Draw-specific scopes remain strict. The reconciler adds
 * {@code RECONCILIATION_REQUIRED} once it can compare projection and transaction watermarks.
 */
@UseCase
@RequiredArgsConstructor
public class GetAnalyticsTrustStateQueryHandler
    implements QueryHandler<GetAnalyticsTrustStateQuery, AnalyticsTrustStateView> {

  private final AnalyticsDailyRepository dailyRepository;
  private final AnalyticsDrawRepository drawRepository;
  private final AnalyticsSellerTerminalDrawRepository sellerTerminalDrawRepository;
  private final AnalyticsVisibilityOverrideRepository visibilityOverrideRepository;
  private final QueryBus queryBus;
  private final TenantZoneApi tenantZoneApi;
  private final Clock clock;

  @Override
  public AnalyticsTrustStateView handle(GetAnalyticsTrustStateQuery query) {
    AnalyticsTrustScope scope = query.scope();
    Instant checkedAt = clock.instant();
    if (isDisabled(scope)) {
      return AnalyticsTrustStateView.disabled(scope, checkedAt);
    }
    Set<LocalDate> availableDates = availableDates(scope);
    Set<LocalDate> sourceActivityDates = sourceActivityDates(scope);
    List<LocalDate> missingDates =
        scope
            .from()
            .datesUntil(scope.to().plusDays(1))
            .filter(
                date ->
                    !availableDates.contains(date)
                        && (!isSourceBackedScope(scope) || sourceActivityDates.contains(date)))
            .toList();
    return missingDates.isEmpty()
        ? AnalyticsTrustStateView.ready(scope, checkedAt)
        : AnalyticsTrustStateView.unavailable(scope, missingDates, checkedAt);
  }

  private boolean isDisabled(AnalyticsTrustScope scope) {
    return visibilityOverrideRepository.existsDisabledForScope(
        scope.type(),
        scope.tenantId() == null ? null : scope.tenantId().value(),
        scope.sellerTerminalId() == null ? null : scope.sellerTerminalId().value(),
        scope.drawId() == null ? null : scope.drawId().value(),
        scope.from(),
        scope.to());
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

  private Set<LocalDate> sourceActivityDates(AnalyticsTrustScope scope) {
    if (!isSourceBackedScope(scope)) {
      return Set.of();
    }

    ZoneId zone = tenantZoneApi.resolveTenantZone(scope.tenantId());
    if (zone == null) zone = ZoneOffset.UTC;
    var from = scope.from().atStartOfDay(zone).toInstant();
    var to = scope.to().plusDays(1).atStartOfDay(zone).toInstant();
    SellerTerminalId terminalId = scope.sellerTerminalId();
    Set<LocalDate> dates =
        queryBus.ask(
            new GetSalesAnalyticsActivityDatesQuery(scope.tenantId(), terminalId, from, to, zone));
    return dates != null ? new HashSet<>(dates) : Set.of();
  }

  private static boolean isSourceBackedScope(AnalyticsTrustScope scope) {
    return scope.type() == AnalyticsTrustScopeType.TENANT
        || scope.type() == AnalyticsTrustScopeType.SELLER_TERMINAL;
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
