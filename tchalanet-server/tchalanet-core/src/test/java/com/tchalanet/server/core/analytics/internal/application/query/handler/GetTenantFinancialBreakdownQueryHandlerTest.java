package com.tchalanet.server.core.analytics.internal.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.query.GetTenantFinancialBreakdownQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetTenantFinancialBreakdownQueryHandlerTest {

  private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final UUID SELLER_TERMINAL_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID DRAW_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final LocalDate FROM = LocalDate.parse("2026-06-25");
  private static final LocalDate TO = LocalDate.parse("2026-06-25");

  private final AnalyticsDailyRepository dailyRepository = mock(AnalyticsDailyRepository.class);
  private final AnalyticsDrawRepository drawRepository = mock(AnalyticsDrawRepository.class);
  private final AnalyticsSellerTerminalDrawRepository sellerTerminalDrawRepository =
      mock(AnalyticsSellerTerminalDrawRepository.class);
  private final GetTenantFinancialBreakdownQueryHandler handler =
      new GetTenantFinancialBreakdownQueryHandler(
          dailyRepository,
          drawRepository,
          sellerTerminalDrawRepository);

  @Test
  void returnsEmptyFinancialViewForNewTenantWithoutAnalyticsRows() {
    when(dailyRepository.findTenantRows(TENANT_ID.value(), FROM, TO)).thenReturn(List.of());
    when(drawRepository.findByTenantIdAndRefDateBetweenOrderByRefDate(TENANT_ID.value(), FROM, TO))
        .thenReturn(List.of());
    when(dailyRepository.findSellerTerminalRows(TENANT_ID.value(), FROM, TO)).thenReturn(List.of());
    when(sellerTerminalDrawRepository.findByTenantIdAndRefDateBetweenOrderByRefDateDescUpdatedAtDesc(
        TENANT_ID.value(), FROM, TO)).thenReturn(List.of());

    var result = handler.handle(new GetTenantFinancialBreakdownQuery(TENANT_ID, FROM, TO, 50, 50));

    assertThat(result.summary().ticketsSold()).isZero();
    assertThat(result.summary().grossSales()).isEqualByComparingTo("0");
    assertThat(result.summary().sellerCommission()).isEqualByComparingTo("0");
    assertThat(result.summary().promotionPotentialPayout()).isEqualByComparingTo("0");
    assertThat(result.dailyRows()).isEmpty();
    assertThat(result.drawRows()).isEmpty();
    assertThat(result.sellerTerminalDailyRows()).isEmpty();
    assertThat(result.sellerTerminalDrawRows()).isEmpty();
  }

  @Test
  void returnsTenantFinancialBreakdownIncludingSellerTerminalDrawRows() {
    when(dailyRepository.findTenantRows(TENANT_ID.value(), FROM, TO))
        .thenReturn(List.of(tenantDailyRow()));
    when(drawRepository.findByTenantIdAndRefDateBetweenOrderByRefDate(TENANT_ID.value(), FROM, TO))
        .thenReturn(List.of(drawRow()));
    when(dailyRepository.findSellerTerminalRows(TENANT_ID.value(), FROM, TO))
        .thenReturn(List.of(sellerTerminalDailyRow()));
    when(sellerTerminalDrawRepository.findByTenantIdAndRefDateBetweenOrderByRefDateDescUpdatedAtDesc(
        TENANT_ID.value(), FROM, TO)).thenReturn(List.of(sellerTerminalDrawRow()));

    var result = handler.handle(new GetTenantFinancialBreakdownQuery(TENANT_ID, FROM, TO, 50, 50));

    assertThat(result.summary().ticketsSold()).isEqualTo(3L);
    assertThat(result.summary().grossSales()).isEqualByComparingTo("90.00");
    assertThat(result.summary().sellerCommission()).isEqualByComparingTo("9.00");
    assertThat(result.summary().tenantCharges()).isEqualByComparingTo("3.00");
    assertThat(result.summary().promotionLines()).isEqualTo(2L);
    assertThat(result.summary().netRevenueEstimated()).isEqualByComparingTo("58.00");

    assertThat(result.drawRows()).hasSize(1);
    assertThat(result.drawRows().getFirst().drawId()).isEqualTo(DRAW_ID);
    assertThat(result.drawRows().getFirst().sellerCommission()).isEqualByComparingTo("4.00");

    assertThat(result.sellerTerminalDailyRows()).hasSize(1);
    assertThat(result.sellerTerminalDailyRows().getFirst().sellerTerminalId()).isEqualTo(SELLER_TERMINAL_ID);

    assertThat(result.sellerTerminalDrawRows()).hasSize(1);
    assertThat(result.sellerTerminalDrawRows().getFirst().sellerTerminalId()).isEqualTo(SELLER_TERMINAL_ID);
    assertThat(result.sellerTerminalDrawRows().getFirst().drawId()).isEqualTo(DRAW_ID);
    assertThat(result.sellerTerminalDrawRows().getFirst().sellerCommission()).isEqualByComparingTo("4.00");
  }

  private static AnalyticsDailyEntity tenantDailyRow() {
    return AnalyticsDailyEntity.builder()
        .tenantId(TENANT_ID.value())
        .dimensionType("TENANT")
        .refDate(FROM)
        .ticketsSoldCount(3L)
        .grossSalesCents(9000L)
        .winningsCalculatedCents(2000L)
        .payoutsPaidCents(1000L)
        .sellerCommissionCents(900L)
        .buyerChargeCents(500L)
        .sellerChargeCents(100L)
        .tenantChargeCents(300L)
        .waivedChargeCents(200L)
        .promotionLineCount(2L)
        .promotionPricedLineCount(1L)
        .promotionPayoutBaseCents(1000L)
        .promotionPotentialPayoutCents(10000L)
        .netRevenueEstimatedCents(5800L)
        .netRevenuePaidBasisCents(6800L)
        .build();
  }

  private static AnalyticsDrawEntity drawRow() {
    return AnalyticsDrawEntity.builder()
        .tenantId(TENANT_ID.value())
        .drawId(DRAW_ID)
        .refDate(FROM)
        .scheduledAt(Instant.parse("2026-06-25T12:00:00Z"))
        .gameCode("HT_BOLET")
        .drawChannelCode("NY")
        .ticketsSoldCount(2L)
        .grossSalesCents(4000L)
        .winningsCalculatedCents(500L)
        .payoutsPaidCents(200L)
        .sellerCommissionCents(400L)
        .buyerChargeCents(100L)
        .sellerChargeCents(0L)
        .tenantChargeCents(50L)
        .waivedChargeCents(0L)
        .promotionLineCount(1L)
        .promotionPricedLineCount(0L)
        .promotionPayoutBaseCents(500L)
        .promotionPotentialPayoutCents(5000L)
        .netRevenueEstimatedCents(3050L)
        .netRevenuePaidBasisCents(3350L)
        .build();
  }

  private static AnalyticsDailyEntity sellerTerminalDailyRow() {
    return AnalyticsDailyEntity.builder()
        .tenantId(TENANT_ID.value())
        .dimensionType("SELLER_TERMINAL")
        .dimensionId(SELLER_TERMINAL_ID)
        .refDate(FROM)
        .ticketsSoldCount(2L)
        .grossSalesCents(4000L)
        .sellerCommissionCents(400L)
        .buyerChargeCents(100L)
        .sellerChargeCents(0L)
        .tenantChargeCents(50L)
        .waivedChargeCents(0L)
        .promotionLineCount(1L)
        .promotionPricedLineCount(0L)
        .promotionPayoutBaseCents(500L)
        .promotionPotentialPayoutCents(5000L)
        .netRevenueEstimatedCents(3050L)
        .netRevenuePaidBasisCents(3350L)
        .build();
  }

  private static AnalyticsSellerTerminalDrawEntity sellerTerminalDrawRow() {
    return AnalyticsSellerTerminalDrawEntity.builder()
        .tenantId(TENANT_ID.value())
        .sellerTerminalId(SELLER_TERMINAL_ID)
        .drawId(DRAW_ID)
        .refDate(FROM)
        .scheduledAt(Instant.parse("2026-06-25T12:00:00Z"))
        .gameCode("HT_BOLET")
        .drawChannelCode("NY")
        .ticketsSoldCount(2L)
        .grossSalesCents(4000L)
        .winningsCalculatedCents(500L)
        .payoutsPaidCents(200L)
        .sellerCommissionCents(400L)
        .buyerChargeCents(100L)
        .sellerChargeCents(0L)
        .tenantChargeCents(50L)
        .waivedChargeCents(0L)
        .promotionLineCount(1L)
        .promotionPricedLineCount(0L)
        .promotionPayoutBaseCents(500L)
        .promotionPotentialPayoutCents(5000L)
        .netRevenueEstimatedCents(3050L)
        .netRevenuePaidBasisCents(3350L)
        .build();
  }
}
