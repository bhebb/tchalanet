package com.tchalanet.server.core.analytics.internal.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustState;
import com.tchalanet.server.core.analytics.api.query.GetAnalyticsTrustStateQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetAnalyticsTrustStateQueryHandlerTest {

  private final AnalyticsDailyRepository dailyRepository =
      Mockito.mock(AnalyticsDailyRepository.class);
  private final AnalyticsDrawRepository drawRepository =
      Mockito.mock(AnalyticsDrawRepository.class);
  private final AnalyticsSellerTerminalDrawRepository sellerTerminalDrawRepository =
      Mockito.mock(AnalyticsSellerTerminalDrawRepository.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-20T12:00:00Z"), ZoneOffset.UTC);
  private final GetAnalyticsTrustStateQueryHandler handler =
      new GetAnalyticsTrustStateQueryHandler(
          dailyRepository, drawRepository, sellerTerminalDrawRepository, clock);

  @Test
  void tenantScopeIsReadyWhenEveryBusinessDateHasAProjection() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var from = LocalDate.of(2026, 7, 17);
    var to = LocalDate.of(2026, 7, 18);
    when(dailyRepository.findTenantRows(tenantId.value(), from, to))
        .thenReturn(List.of(daily(from), daily(to)));

    var result =
        handler.handle(
            new GetAnalyticsTrustStateQuery(AnalyticsTrustScope.tenant(tenantId, from, to)));

    assertThat(result.state()).isEqualTo(AnalyticsTrustState.READY);
    assertThat(result.missingBusinessDates()).isEmpty();
    assertThat(result.checkedAt()).isEqualTo(Instant.parse("2026-07-20T12:00:00Z"));
  }

  @Test
  void missingProjectionDateIsUnavailableRatherThanZero() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var from = LocalDate.of(2026, 7, 17);
    var to = LocalDate.of(2026, 7, 18);
    when(dailyRepository.findTenantRows(tenantId.value(), from, to))
        .thenReturn(List.of(daily(from)));

    var result =
        handler.handle(
            new GetAnalyticsTrustStateQuery(AnalyticsTrustScope.tenant(tenantId, from, to)));

    assertThat(result.state()).isEqualTo(AnalyticsTrustState.UNAVAILABLE);
    assertThat(result.reasonCode()).isEqualTo("analytics.trust.projection_missing");
    assertThat(result.missingBusinessDates()).containsExactly(to);
  }

  @Test
  void platformScopeUsesPlatformProjectionCoverage() {
    var refDate = LocalDate.of(2026, 7, 17);
    when(dailyRepository.findPlatformRows(refDate, refDate)).thenReturn(List.of(daily(refDate)));

    var result =
        handler.handle(
            new GetAnalyticsTrustStateQuery(AnalyticsTrustScope.platform(refDate, refDate)));

    assertThat(result.state()).isEqualTo(AnalyticsTrustState.READY);
  }

  @Test
  void sellerTerminalScopeUsesTheTerminalDimension() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var sellerTerminalId = SellerTerminalId.of(UUID.randomUUID());
    var refDate = LocalDate.of(2026, 7, 17);
    when(dailyRepository.findSellerTerminalRows(
            tenantId.value(), sellerTerminalId.value(), refDate, refDate))
        .thenReturn(List.of(daily(refDate)));

    var result =
        handler.handle(
            new GetAnalyticsTrustStateQuery(
                AnalyticsTrustScope.sellerTerminal(tenantId, sellerTerminalId, refDate, refDate)));

    assertThat(result.state()).isEqualTo(AnalyticsTrustState.READY);
  }

  @Test
  void sellerTerminalDrawScopeUsesTheTerminalAndDrawProjection() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var sellerTerminalId = SellerTerminalId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var refDate = LocalDate.of(2026, 7, 17);
    when(sellerTerminalDrawRepository.findByTenantIdAndSellerTerminalIdAndDrawIdAndRefDate(
            tenantId.value(), sellerTerminalId.value(), drawId.value(), refDate))
        .thenReturn(List.of(sellerTerminalDraw(refDate)));

    var result =
        handler.handle(
            new GetAnalyticsTrustStateQuery(
                AnalyticsTrustScope.sellerTerminalDraw(
                    tenantId, sellerTerminalId, drawId, refDate)));

    assertThat(result.state()).isEqualTo(AnalyticsTrustState.READY);
  }

  @Test
  void drawScopeDoesNotTreatAnotherDrawAsCoverage() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var refDate = LocalDate.of(2026, 7, 17);
    when(drawRepository.findByTenantIdAndDrawIdAndRefDateBetweenOrderByRefDate(
            tenantId.value(), drawId.value(), refDate, refDate))
        .thenReturn(List.of());

    var result =
        handler.handle(
            new GetAnalyticsTrustStateQuery(AnalyticsTrustScope.draw(tenantId, drawId, refDate)));

    assertThat(result.state()).isEqualTo(AnalyticsTrustState.UNAVAILABLE);
    assertThat(result.missingBusinessDates()).containsExactly(refDate);
  }

  private static AnalyticsDailyEntity daily(LocalDate refDate) {
    return AnalyticsDailyEntity.builder().refDate(refDate).build();
  }

  private static AnalyticsSellerTerminalDrawEntity sellerTerminalDraw(LocalDate refDate) {
    return AnalyticsSellerTerminalDrawEntity.builder().refDate(refDate).build();
  }
}
