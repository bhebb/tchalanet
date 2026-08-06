package com.tchalanet.server.features.pos.tickets.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustStateView;
import com.tchalanet.server.core.analytics.api.query.GetAnalyticsTrustStateQuery;
import com.tchalanet.server.core.analytics.api.query.GetCashierDashboardStatsQuery;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.features.pos.tickets.mapper.PosTicketMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

class PosTicketsServiceTest {

  @AfterEach
  void clearResponseContext() {
    ApiResponseContext.clear();
  }

  @Test
  void doesNotExposeZeroStatsWhenTheTerminalProjectionIsUnavailable() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var terminalId = SellerTerminalId.of(UUID.randomUUID());
    var targetDate = LocalDate.of(2026, 7, 20);
    var scope = AnalyticsTrustScope.sellerTerminal(tenantId, terminalId, targetDate, targetDate);
    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any(GetAnalyticsTrustStateQuery.class)))
        .thenReturn(
            AnalyticsTrustStateView.unavailable(
                scope, java.util.List.of(targetDate), Instant.now()));

    var response =
        service(queryBus).sellerTerminalStats(context(tenantId, terminalId), targetDate.toString());

    assertThat(response.available()).isFalse();
    assertThat(response.trustState()).isEqualTo("UNAVAILABLE");
    assertThat(response.ticketCount()).isZero();
    assertThat(response.breakdown()).isEmpty();
    assertThat(ApiResponseContext.get().getNotices())
        .singleElement()
        .extracting(notice -> notice.code())
        .isEqualTo("pos.dashboard.analytics_unavailable");
    verify(queryBus).ask(new GetAnalyticsTrustStateQuery(scope));
    verify(queryBus, never()).ask(any(GetCashierDashboardStatsQuery.class));
  }

  private static PosTicketsService service(QueryBus queryBus) {
    return new PosTicketsService(
        queryBus,
        mock(CommandBus.class),
        mock(PosTicketMapper.class),
        mock(TicketScanResolver.class));
  }

  @Test
  void pinsTheTicketListToTheCallingTerminalEvenWhenAnotherTerminalIsRequested() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var callerTerminal = SellerTerminalId.of(UUID.randomUUID());
    var otherTerminal = SellerTerminalId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any(ListTicketsQuery.class))).thenReturn(emptyPage());
    var ctx = context(tenantId, callerTerminal);
    when(ctx.sellerTerminalId()).thenReturn(callerTerminal);

    service(queryBus)
        .listTickets(
            ctx,
            otherTerminal,
            drawId,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 20));

    var captor = ArgumentCaptor.forClass(ListTicketsQuery.class);
    verify(queryBus).ask(captor.capture());
    // RLS only isolates tenants, so the terminal filter is what keeps one
    // seller from reading another seller's tickets.
    assertThat(captor.getValue().sellerTerminalId()).isEqualTo(callerTerminal);
    assertThat(captor.getValue().drawId()).isEqualTo(drawId);
  }

  @Test
  void keepsTheRequestedTerminalFilterForCallersThatAreNotATerminal() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var requestedTerminal = SellerTerminalId.of(UUID.randomUUID());
    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any(ListTicketsQuery.class))).thenReturn(emptyPage());
    var ctx = context(tenantId, null);
    when(ctx.sellerTerminalId()).thenReturn(null);

    service(queryBus)
        .listTickets(
            ctx, requestedTerminal, null, null, null, null, null, PageRequest.of(0, 20));

    var captor = ArgumentCaptor.forClass(ListTicketsQuery.class);
    verify(queryBus).ask(captor.capture());
    assertThat(captor.getValue().sellerTerminalId()).isEqualTo(requestedTerminal);
  }

  private static <T> TchPage<T> emptyPage() {
    return TchPage.of(java.util.List.of(), 0, 20, 0L, 0, true, false, false);
  }

  private static TchRequestContext context(TenantId tenantId, SellerTerminalId terminalId) {
    var context = mock(TchRequestContext.class);
    when(context.tenantZoneId()).thenReturn(ZoneId.of("America/Port-au-Prince"));
    when(context.tenantCurrency()).thenReturn(Currency.getInstance("HTG"));
    when(context.effectiveTenantIdRequired()).thenReturn(tenantId);
    when(context.sellerTerminalIdRequired()).thenReturn(terminalId);
    return context;
  }
}
