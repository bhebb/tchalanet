package com.tchalanet.server.core.analytics.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.core.analytics.api.command.PurgeAnalyticsCommand;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSelectionRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import org.junit.jupiter.api.Test;

class PurgeAnalyticsCommandHandlerTest {

  @Test
  void dryRunCountsAllDerivedProjections() {
    var daily = mock(AnalyticsDailyRepository.class);
    var draw = mock(AnalyticsDrawRepository.class);
    var selection = mock(AnalyticsSelectionRepository.class);
    var sellerTerminalDraw = mock(AnalyticsSellerTerminalDrawRepository.class);
    when(daily.countOlderThan(any())).thenReturn(11L);
    when(draw.countOlderThan(any())).thenReturn(12L);
    when(selection.countOlderThan(any())).thenReturn(13L);
    when(sellerTerminalDraw.countOlderThan(any())).thenReturn(14L);

    var result =
        newHandler(daily, draw, selection, sellerTerminalDraw)
            .handle(new PurgeAnalyticsCommand(true));

    assertThat(result.dailyRows()).isEqualTo(11L);
    assertThat(result.drawRows()).isEqualTo(12L);
    assertThat(result.selectionRows()).isEqualTo(13L);
    assertThat(result.sellerTerminalDrawRows()).isEqualTo(14L);
    assertThat(result.dryRun()).isTrue();
    verify(daily).countOlderThan(any());
    verify(draw).countOlderThan(any());
    verify(selection).countOlderThan(any());
    verify(sellerTerminalDraw).countOlderThan(any());
  }

  @Test
  void purgeDeletesAllDerivedProjections() {
    var daily = mock(AnalyticsDailyRepository.class);
    var draw = mock(AnalyticsDrawRepository.class);
    var selection = mock(AnalyticsSelectionRepository.class);
    var sellerTerminalDraw = mock(AnalyticsSellerTerminalDrawRepository.class);
    when(daily.deleteOlderThan(any())).thenReturn(21);
    when(draw.deleteOlderThan(any())).thenReturn(22);
    when(selection.deleteOlderThan(any())).thenReturn(23);
    when(sellerTerminalDraw.deleteOlderThan(any())).thenReturn(24);

    var result =
        newHandler(daily, draw, selection, sellerTerminalDraw)
            .handle(new PurgeAnalyticsCommand(false));

    assertThat(result.dailyRows()).isEqualTo(21L);
    assertThat(result.drawRows()).isEqualTo(22L);
    assertThat(result.selectionRows()).isEqualTo(23L);
    assertThat(result.sellerTerminalDrawRows()).isEqualTo(24L);
    assertThat(result.dryRun()).isFalse();
    verify(daily).deleteOlderThan(any());
    verify(draw).deleteOlderThan(any());
    verify(selection).deleteOlderThan(any());
    verify(sellerTerminalDraw).deleteOlderThan(any());
  }

  @Test
  void clampsNonPositiveRetentionToOneMonth() {
    var handler =
        new PurgeAnalyticsCommandHandler(
            mock(AnalyticsDailyRepository.class),
            mock(AnalyticsDrawRepository.class),
            mock(AnalyticsSelectionRepository.class),
            mock(AnalyticsSellerTerminalDrawRepository.class),
            0,
            24,
            24,
            24);

    assertThat(handler).isNotNull();
  }

  private static PurgeAnalyticsCommandHandler newHandler(
      AnalyticsDailyRepository daily,
      AnalyticsDrawRepository draw,
      AnalyticsSelectionRepository selection,
      AnalyticsSellerTerminalDrawRepository sellerTerminalDraw) {
    return new PurgeAnalyticsCommandHandler(
        daily, draw, selection, sellerTerminalDraw, 24, 24, 24, 24);
  }
}
