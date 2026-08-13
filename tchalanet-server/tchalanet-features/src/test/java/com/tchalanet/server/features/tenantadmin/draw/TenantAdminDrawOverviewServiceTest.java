package com.tchalanet.server.features.tenantadmin.draw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.limitpolicy.api.query.ExposureAlertItemView;
import com.tchalanet.server.core.limitpolicy.api.query.ExposureAlertsOverviewView;
import com.tchalanet.server.core.sales.api.query.DrawTopSelectionsView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantAdminDrawOverviewServiceTest {

  @Test
  void getOverviewAssemblesTopSelectionsAndExposureAlerts() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var channelId = DrawChannelId.of(UUID.randomUUID());

    var drawSummary =
        new DrawSummary(
            drawId,
            tenantId,
            LocalDate.of(2026, 7, 19),
            DrawStatus.OPEN,
            null,
            null,
            null,
            null,
            null,
            null,
            channelId,
            "GA_EVE",
            "Georgia Eve",
            LocalTime.NOON,
            "UTC",
            true,
            null,
            null,
            null,
            null,
            null,
            true,
            null);

    var topSelItem =
        new DrawTopSelectionsView.SelectionItem(1, "42", "BORLETTE", "MATCH_1_2D", null, 15, 7500);
    var topSelectionsView = new DrawTopSelectionsView(drawId, List.of(topSelItem));

    var alertItem =
        new ExposureAlertItemView(
            BetType.MATCH_1_2D,
            "42",
            BigDecimal.valueOf(750),
            15,
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(0.75));
    var alertsView =
        new ExposureAlertsOverviewView(tenantId, drawId, "channel", List.of(alertItem));

    var queryBus = mock(QueryBus.class);
    // call order: GetDrawByIdQuery, ListDrawTopSelectionsQuery,
    // GetEffectiveLimitsForDrawQuery (null), GetExposureAlertsOverviewQuery
    when(queryBus.ask(any()))
        .thenReturn(drawSummary)
        .thenReturn(topSelectionsView)
        .thenReturn(null)
        .thenReturn(alertsView);

    var service = new TenantAdminDrawOverviewService(queryBus);
    var result = service.getOverview(ctx(tenantId), drawId);

    assertThat(result.draw().drawId()).isEqualTo(drawId.value());
    assertThat(result.draw().channelCode()).isEqualTo("GA_EVE");
    assertThat(result.draw().status()).isEqualTo(DrawStatus.OPEN);

    assertThat(result.topSelections()).hasSize(1);
    assertThat(result.topSelections().getFirst().displaySelection()).isEqualTo("42");
    assertThat(result.topSelections().getFirst().count()).isEqualTo(15);
    assertThat(result.topSelections().getFirst().totalStakeCents()).isEqualTo(7500);

    assertThat(result.effectiveLimits()).isEmpty();

    assertThat(result.exposureAlerts()).hasSize(1);
    assertThat(result.exposureAlerts().getFirst().selectionKey()).isEqualTo("42");
    assertThat(result.exposureAlerts().getFirst().betType()).isEqualTo("MATCH_1_2D");
    assertThat(result.exposureAlerts().getFirst().stakeRatio())
        .isEqualByComparingTo(BigDecimal.valueOf(0.75));
  }

  @Test
  void getOverviewReturnsEmptyListsWhenTopSelectionsAndAlertsAreNull() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var channelId = DrawChannelId.of(UUID.randomUUID());

    var drawSummary =
        new DrawSummary(
            drawId,
            tenantId,
            LocalDate.of(2026, 7, 19),
            DrawStatus.OPEN,
            null,
            null,
            null,
            null,
            null,
            null,
            channelId,
            "GA_EVE",
            "Georgia Eve",
            LocalTime.NOON,
            "UTC",
            true,
            null,
            null,
            null,
            null,
            null,
            true,
            null);

    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any()))
        .thenReturn(drawSummary)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null);

    var service = new TenantAdminDrawOverviewService(queryBus);
    var result = service.getOverview(ctx(tenantId), drawId);

    assertThat(result.topSelections()).isEmpty();
    assertThat(result.effectiveLimits()).isEmpty();
    assertThat(result.exposureAlerts()).isEmpty();
  }

  @Test
  void getOverviewReturnsCorrectDrawInfoForClosedDraw() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var channelId = DrawChannelId.of(UUID.randomUUID());
    var drawDate = LocalDate.of(2026, 7, 19);

    var drawSummary =
        new DrawSummary(
            drawId,
            tenantId,
            drawDate,
            DrawStatus.CLOSED,
            null,
            null,
            null,
            null,
            null,
            null,
            channelId,
            "GA_EVE",
            "Georgia Eve",
            LocalTime.NOON,
            "UTC",
            true,
            null,
            null,
            null,
            null,
            null,
            true,
            null);

    var topSelItem =
        new DrawTopSelectionsView.SelectionItem(1, "07", "BORLETTE", "MATCH_1_2D", null, 8, 4000);
    var topSelectionsView = new DrawTopSelectionsView(drawId, List.of(topSelItem));

    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any()))
        .thenReturn(drawSummary)
        .thenReturn(topSelectionsView)
        .thenReturn(null)
        .thenReturn(null);

    var service = new TenantAdminDrawOverviewService(queryBus);
    var result = service.getOverview(ctx(tenantId), drawId);

    assertThat(result.draw().status()).isEqualTo(DrawStatus.CLOSED);
    assertThat(result.draw().drawDate()).isEqualTo(drawDate);
    assertThat(result.draw().channelLabel()).isEqualTo("Georgia Eve");
    assertThat(result.topSelections()).hasSize(1);
    assertThat(result.topSelections().getFirst().displaySelection()).isEqualTo("07");
    assertThat(result.exposureAlerts()).isEmpty();
  }

  private static TchRequestContext ctx(TenantId tenantId) {
    return new TchRequestContext(
        "tenant",
        tenantId.value(),
        "tenant",
        tenantId.value(),
        null,
        Set.of(),
        Set.of(),
        null,
        "request-id",
        null,
        null,
        false,
        null,
        "active",
        null,
        null,
        tenantId,
        ZoneId.of("America/Port-au-Prince"),
        null,
        null,
        null,
        null,
        Set.of(),
        Set.of(),
        null);
  }
}
