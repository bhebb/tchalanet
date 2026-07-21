package com.tchalanet.server.core.analytics.internal.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.query.GetCashierDashboardStatsQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetCashierDashboardStatsQueryHandlerTest {

  @Test
  void readsTheSellerTerminalAnalyticsDimensions() {
    var dailyRepository = Mockito.mock(AnalyticsDailyRepository.class);
    var drawRepository = Mockito.mock(AnalyticsSellerTerminalDrawRepository.class);
    var tenantId = TenantId.of(UUID.randomUUID());
    var sellerTerminalId = SellerTerminalId.of(UUID.randomUUID());
    var drawId = UUID.randomUUID();
    var refDate = LocalDate.of(2026, 7, 17);

    when(dailyRepository.findSellerTerminalRow(tenantId.value(), sellerTerminalId.value(), refDate))
        .thenReturn(
            Optional.of(
                AnalyticsDailyEntity.builder()
                    .ticketsSoldCount(54L)
                    .grossSalesCents(543100L)
                    .winningsCalculatedCents(1000L)
                    .sellerCommissionCents(70603L)
                    .netRevenueEstimatedCents(472497L)
                    .build()));
    when(drawRepository.findByTenantIdAndSellerTerminalIdAndRefDateOrderByScheduledAtAsc(
            tenantId.value(), sellerTerminalId.value(), refDate))
        .thenReturn(
            List.of(
                AnalyticsSellerTerminalDrawEntity.builder()
                    .drawId(drawId)
                    .drawChannelCode("NY_EVE")
                    .ticketsSoldCount(54L)
                    .grossSalesCents(543100L)
                    .build()));

    var handler = new GetCashierDashboardStatsQueryHandler(dailyRepository, drawRepository);

    var result =
        handler.handle(new GetCashierDashboardStatsQuery(tenantId, sellerTerminalId, refDate));

    assertThat(result.today().ticketsSold()).isEqualTo(54L);
    assertThat(result.today().grossSales()).hasToString("5431.00");
    assertThat(result.today().sellerCommission()).hasToString("706.03");
    assertThat(result.today().netRevenueEstimated()).hasToString("4724.97");
    assertThat(result.drawBreakdown())
        .singleElement()
        .satisfies(
            draw -> {
              assertThat(draw.drawId()).isEqualTo(drawId);
              assertThat(draw.drawChannelCode()).isEqualTo("NY_EVE");
              assertThat(draw.ticketsSold()).isEqualTo(54L);
            });
    verify(dailyRepository)
        .findSellerTerminalRow(tenantId.value(), sellerTerminalId.value(), refDate);
  }
}
