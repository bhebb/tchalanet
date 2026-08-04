package com.tchalanet.server.core.analytics.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.command.ReconcileAnalyticsCommand;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationMode;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationStatus;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSelectionEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSelectionRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsTenantProjectionLock;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketLineSnapshot;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import com.tchalanet.server.platform.tenant.api.TenantZoneApi;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AnalyticsReconciliationServiceTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final UUID SELLER_TERMINAL_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID DRAW_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID DRAW_CHANNEL_ID =
      UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 8, 3);
  private static final Instant SOLD_AT = Instant.parse("2026-08-03T14:00:00Z");
  private static final LocalDate DRAW_DATE = BUSINESS_DATE;

  @Test
  void validateReportsEveryMissingProjectionForAnApprovedSale() {
    var fixture = fixture(List.of(approvedTicket()));

    var result = fixture.service().validate(command(AnalyticsReconciliationMode.VALIDATE));

    assertThat(result.status()).isEqualTo(AnalyticsReconciliationStatus.MISMATCH);
    assertThat(result.mismatches())
        .extracting(mismatch -> mismatch.projection())
        .containsExactlyInAnyOrder(
            "DAILY_TENANT",
            "DAILY_SELLER_TERMINAL",
            "DRAW",
            "SELLER_TERMINAL_DRAW",
            "SELECTION:12");
    assertThat(result.mismatches())
        .filteredOn(mismatch -> mismatch.projection().equals("DAILY_TENANT"))
        .singleElement()
        .satisfies(
            mismatch -> {
              assertThat(mismatch.expected().ticketsSold()).isEqualTo(1);
              assertThat(mismatch.expected().grossSalesMinor()).isEqualTo(1_000L);
              assertThat(mismatch.expected().sellerCommissionMinor()).isEqualTo(130L);
            });
    verify(fixture.alertService()).onReconciliationMismatch(result);
  }

  @Test
  void rebuildReplacesTheSelectedTenantWindowAndValidatesTheWrittenRows() {
    var fixture = fixture(List.of(approvedTicket()));
    var dailyRows = new AtomicReference<List<AnalyticsDailyEntity>>(List.of());
    var drawRows = new AtomicReference<List<AnalyticsDrawEntity>>(List.of());
    var sellerTerminalDrawRows =
        new AtomicReference<List<AnalyticsSellerTerminalDrawEntity>>(List.of());
    var selectionRows = new AtomicReference<List<AnalyticsSelectionEntity>>(List.of());

    captureSavedRows(fixture.dailyRepository(), dailyRows);
    captureSavedRows(fixture.drawRepository(), drawRows);
    captureSavedRows(fixture.sellerTerminalDrawRepository(), sellerTerminalDrawRows);
    captureSavedRows(fixture.selectionRepository(), selectionRows);
    when(fixture
            .dailyRepository()
            .findTenantRows(eq(TENANT_ID.value()), eq(BUSINESS_DATE), eq(BUSINESS_DATE)))
        .thenAnswer(
            ignored ->
                dailyRows.get().stream()
                    .filter(row -> "TENANT".equals(row.getDimensionType()))
                    .toList());
    when(fixture
            .dailyRepository()
            .findSellerTerminalRows(eq(TENANT_ID.value()), eq(BUSINESS_DATE), eq(BUSINESS_DATE)))
        .thenAnswer(
            ignored ->
                dailyRows.get().stream()
                    .filter(row -> "SELLER_TERMINAL".equals(row.getDimensionType()))
                    .toList());
    when(fixture
            .drawRepository()
            .findByTenantIdAndRefDateBetweenOrderByRefDate(
                eq(TENANT_ID.value()), eq(BUSINESS_DATE), eq(BUSINESS_DATE)))
        .thenAnswer(ignored -> drawRows.get());
    when(fixture
            .sellerTerminalDrawRepository()
            .findByTenantIdAndRefDateBetweenOrderByRefDateDescUpdatedAtDesc(
                eq(TENANT_ID.value()), eq(BUSINESS_DATE), eq(BUSINESS_DATE)))
        .thenAnswer(ignored -> sellerTerminalDrawRows.get());
    when(fixture
            .selectionRepository()
            .findByTenantIdAndRefDateBetweenOrderByRefDate(
                eq(TENANT_ID.value()), eq(BUSINESS_DATE), eq(BUSINESS_DATE)))
        .thenAnswer(ignored -> selectionRows.get());

    var result =
        fixture
            .service()
            .rebuildAndValidate(command(AnalyticsReconciliationMode.REBUILD_AND_VALIDATE));

    assertThat(result.status()).isEqualTo(AnalyticsReconciliationStatus.SUCCESS);
    assertThat(result.mismatches()).isEmpty();
    verify(fixture.projectionLock()).acquire(TENANT_ID);
    verify(fixture.alertService()).clearAutomaticUnavailable(result);
    verify(fixture.dailyRepository())
        .deleteTenantProjectionRows(TENANT_ID.value(), BUSINESS_DATE, BUSINESS_DATE);
    verify(fixture.drawRepository())
        .deleteTenantRows(TENANT_ID.value(), BUSINESS_DATE, BUSINESS_DATE);
    verify(fixture.sellerTerminalDrawRepository())
        .deleteTenantRows(TENANT_ID.value(), BUSINESS_DATE, BUSINESS_DATE);
    verify(fixture.selectionRepository())
        .deleteTenantRows(TENANT_ID.value(), BUSINESS_DATE, BUSINESS_DATE);
    assertThat(drawRows.get())
        .singleElement()
        .extracting(AnalyticsDrawEntity::getRefDate)
        .isEqualTo(DRAW_DATE);
    assertThat(sellerTerminalDrawRows.get())
        .singleElement()
        .extracting(AnalyticsSellerTerminalDrawEntity::getRefDate)
        .isEqualTo(DRAW_DATE);
  }

  private static Fixture fixture(List<SalesAnalyticsTicketSnapshot> snapshots) {
    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any())).thenReturn(snapshots);
    var tenantZoneApi = mock(TenantZoneApi.class);
    when(tenantZoneApi.resolveTenantZone(TENANT_ID)).thenReturn(ZoneOffset.UTC);
    var dailyRepository = mock(AnalyticsDailyRepository.class);
    var drawRepository = mock(AnalyticsDrawRepository.class);
    var sellerTerminalDrawRepository = mock(AnalyticsSellerTerminalDrawRepository.class);
    var selectionRepository = mock(AnalyticsSelectionRepository.class);
    var projectionLock = mock(AnalyticsTenantProjectionLock.class);
    var alertService = mock(AnalyticsReconciliationAlertService.class);
    var service =
        new AnalyticsReconciliationService(
            queryBus,
            tenantZoneApi,
            dailyRepository,
            drawRepository,
            sellerTerminalDrawRepository,
            selectionRepository,
            projectionLock,
            alertService,
            Clock.fixed(Instant.parse("2026-08-03T18:00:00Z"), ZoneOffset.UTC));
    return new Fixture(
        service,
        dailyRepository,
        drawRepository,
        sellerTerminalDrawRepository,
        selectionRepository,
        projectionLock,
        alertService);
  }

  private static ReconcileAnalyticsCommand command(AnalyticsReconciliationMode mode) {
    return new ReconcileAnalyticsCommand(
        TENANT_ID,
        BUSINESS_DATE,
        BUSINESS_DATE,
        mode,
        mode == AnalyticsReconciliationMode.REBUILD_AND_VALIDATE
            ? "Repair missing analytics rows"
            : null);
  }

  private static SalesAnalyticsTicketSnapshot approvedTicket() {
    return new SalesAnalyticsTicketSnapshot(
        UUID.fromString("50000000-0000-0000-0000-000000000001"),
        TENANT_ID.value(),
        SELLER_TERMINAL_ID,
        DRAW_ID,
        DRAW_CHANNEL_ID,
        SOLD_AT,
        SOLD_AT.plusSeconds(86_400),
        DRAW_DATE,
        "APPROVED",
        null,
        null,
        "NOT_RESULTED",
        null,
        "PENDING",
        null,
        null,
        BigDecimal.ZERO,
        null,
        null,
        null,
        new BigDecimal("10.00"),
        BigDecimal.ZERO,
        new BigDecimal("1.30"),
        List.of(
            new SalesAnalyticsTicketLineSnapshot(
                "HT_BOLET",
                "MATCH_2_2D",
                (short) 1,
                "12",
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                "CUSTOMER",
                "STANDARD")),
        List.of());
  }

  private static <T> void captureSavedRows(
      org.springframework.data.repository.CrudRepository<T, ?> repository,
      AtomicReference<List<T>> target) {
    doAnswer(
            invocation -> {
              var rows = new java.util.ArrayList<T>();
              invocation.<Iterable<T>>getArgument(0).forEach(rows::add);
              target.set(List.copyOf(rows));
              return rows;
            })
        .when(repository)
        .saveAll(any());
  }

  private record Fixture(
      AnalyticsReconciliationService service,
      AnalyticsDailyRepository dailyRepository,
      AnalyticsDrawRepository drawRepository,
      AnalyticsSellerTerminalDrawRepository sellerTerminalDrawRepository,
      AnalyticsSelectionRepository selectionRepository,
      AnalyticsTenantProjectionLock projectionLock,
      AnalyticsReconciliationAlertService alertService) {}
}
