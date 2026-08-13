package com.tchalanet.server.features.pos.draws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.DrawChannelDisplayFormatter;
import com.tchalanet.server.catalog.drawchannel.api.model.ChannelGamesView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelCalendarRow;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelGameView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSearchCriteria;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.catalog.drawchannel.api.model.GameSummaryView;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.CashierNextDrawView;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.limitpolicy.api.query.ExposureAlertItemView;
import com.tchalanet.server.core.limitpolicy.api.query.ExposureAlertsOverviewView;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PosDrawsServiceTest {

  @Test
  void availableDrawCarriesProviderAndTenantLocalSchedule() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var slotId = ResultSlotId.of(UUID.randomUUID());
    var drawChannelId = DrawChannelId.of(UUID.randomUUID());
    var scheduledAt = Instant.parse("2026-07-19T22:59:00Z");
    var queryBus = mock(QueryBus.class);
    var drawChannelCatalog = mock(DrawChannelCatalog.class);
    var tenantGameApi = mock(TenantGameApi.class);
    var gameCatalog = mock(GameCatalog.class);
    var resultSlotCatalog = mock(ResultSlotCatalog.class);
    when(queryBus.ask(any()))
        .thenReturn(
            List.of(
                new CashierNextDrawView(
                    DrawId.of(UUID.randomUUID()),
                    drawChannelId,
                    slotId,
                    "GA_EVE",
                    "GA_EVE",
                    "Georgia",
                    LocalDate.of(2026, 7, 19),
                    LocalTime.of(18, 59),
                    scheduledAt,
                    scheduledAt.plusSeconds(900),
                    "OPEN")));
    when(drawChannelCatalog.listChannelGames(tenantId))
        .thenReturn(
            List.of(
                new ChannelGamesView(
                    "GA_EVE",
                    List.of(
                        new GameSummaryView(
                            TenantGameId.of(UUID.randomUUID()), "HT_BOLET", true, null)))));
    when(tenantGameApi.listGames(tenantId))
        .thenReturn(
            List.of(
                new TenantGameRefView(
                    TenantGameId.of(UUID.randomUUID()),
                    GameId.of(UUID.randomUUID()),
                    "HT_BOLET",
                    true,
                    true,
                    "Bòlèt",
                    1,
                    null,
                    null)));
    when(gameCatalog.listActive()).thenReturn(List.of(activeGame("HT_BOLET")));
    when(resultSlotCatalog.requireByKey("GA_EVE"))
        .thenReturn(
            new ResultSlotView(
                slotId,
                "GA_EVE",
                "GA",
                ZoneId.of("America/New_York"),
                LocalTime.of(18, 59),
                "SUN",
                true,
                null,
                null,
                null));
    var service =
        new PosDrawsService(
            queryBus,
            drawChannelCatalog,
            tenantGameApi,
            gameCatalog,
            resultSlotCatalog,
            new DrawChannelDisplayFormatter(),
            (id, date) -> new TenantBusinessDayView(id, date, true, null, null),
            new FixedTimeProvider(scheduledAt));

    var availableDraws = service.listAvailable(ctx(tenantId), 24, 20);
    assertThat(availableDraws).hasSize(1);
    var draw = availableDraws.getFirst();

    assertThat(draw.providerDate()).isEqualTo(LocalDate.of(2026, 7, 19));
    assertThat(draw.providerTime()).isEqualTo(LocalTime.of(18, 59));
    assertThat(draw.providerTimezone()).isEqualTo("America/New_York");
    assertThat(draw.localDate()).isEqualTo(LocalDate.of(2026, 7, 19));
    assertThat(draw.localTime()).isEqualTo(LocalTime.of(18, 59));
    assertThat(draw.localTimezone()).isEqualTo("America/Port-au-Prince");
    assertThat(draw.gameCodes()).containsExactly("HT_BOLET");
  }

  @Test
  void availableDrawsExcludeDrawsWithoutSellableGames() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var slotId = ResultSlotId.of(UUID.randomUUID());
    var scheduledAt = Instant.parse("2026-07-19T22:59:00Z");
    var queryBus = mock(QueryBus.class);
    var drawChannelCatalog = mock(DrawChannelCatalog.class);
    var tenantGameApi = mock(TenantGameApi.class);
    var gameCatalog = mock(GameCatalog.class);
    var resultSlotCatalog = mock(ResultSlotCatalog.class);
    when(queryBus.ask(any()))
        .thenReturn(
            List.of(
                new CashierNextDrawView(
                    DrawId.of(UUID.randomUUID()),
                    DrawChannelId.of(UUID.randomUUID()),
                    slotId,
                    "GA_EVE",
                    "GA_EVE",
                    "Georgia",
                    LocalDate.of(2026, 7, 19),
                    LocalTime.of(18, 59),
                    scheduledAt,
                    scheduledAt.plusSeconds(900),
                    "OPEN")));
    when(drawChannelCatalog.listChannelGames(tenantId))
        .thenReturn(
            List.of(
                new ChannelGamesView(
                    "GA_EVE",
                    List.of(
                        new GameSummaryView(
                            TenantGameId.of(UUID.randomUUID()), "HT_BOLET", true, null)))));
    when(tenantGameApi.listGames(tenantId)).thenReturn(List.of());
    when(gameCatalog.listActive()).thenReturn(List.of(activeGame("HT_BOLET")));

    var service =
        new PosDrawsService(
            queryBus,
            drawChannelCatalog,
            tenantGameApi,
            gameCatalog,
            resultSlotCatalog,
            new DrawChannelDisplayFormatter(),
            (id, date) -> new TenantBusinessDayView(id, date, true, null, null),
            new FixedTimeProvider(scheduledAt));

    assertThat(service.listAvailable(ctx(tenantId), 24, 20)).isEmpty();
  }

  @Test
  void tenantClosedBusinessDayReturnsNoAvailableDrawsWithoutQueryingDraws() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var now = Instant.parse("2026-01-02T14:00:00Z");
    var service =
        new PosDrawsService(
            new FailingQueryBus(),
            new EmptyDrawChannelCatalog(),
            mock(TenantGameApi.class),
            mock(GameCatalog.class),
            mock(ResultSlotCatalog.class),
            new DrawChannelDisplayFormatter(),
            (id, date) ->
                new TenantBusinessDayView(id, date, false, "TENANT_CLOSED", "Tenant closed"),
            new FixedTimeProvider(now));

    var result = service.listAvailable(ctx(tenantId), 24, 20);

    assertThat(result).isEmpty();
  }

  private static GameView activeGame(String code) {
    return new GameView(
        GameId.of(UUID.randomUUID()), code, code, "HAITI", null, 2, 5, null, true, 1, null, null);
  }

  @Test
  void getDetailReturnsExposureAlertsAndTopSelectionsWhenAlertViewPresent() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var terminalId = SellerTerminalId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var drawChannelId = DrawChannelId.of(UUID.randomUUID());

    var drawSummary =
        new DrawSummary(
            drawId, tenantId, LocalDate.of(2026, 7, 19), DrawStatus.OPEN, null, null, null, null,
            null, null, drawChannelId, "GA_EVE", "Georgia Eve", LocalTime.NOON, "UTC", true, null,
            null, null, null, null, true, null);

    var alertItem =
        new ExposureAlertItemView(
            BetType.MATCH_1_2D,
            "42",
            BigDecimal.valueOf(500),
            10,
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(0.5));
    var alertsView =
        new ExposureAlertsOverviewView(tenantId, drawId, "terminal", List.of(alertItem));

    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any())).thenReturn(drawSummary).thenReturn(alertsView);

    var service = new PosDrawsService(queryBus, null, null, null, null, null);
    var result = service.getDetail(ctxWithTerminal(tenantId, terminalId), drawId);

    assertThat(result.drawId()).isEqualTo(drawId.value());
    assertThat(result.exposure().active()).isTrue();
    assertThat(result.exposure().limitConfigured()).isTrue();
    assertThat(result.exposure().alerts()).hasSize(1);
    assertThat(result.exposure().alerts().getFirst().selectionKey()).isEqualTo("42");
    assertThat(result.topSelections()).hasSize(1);
    assertThat(result.topSelections().getFirst().selectionKey()).isEqualTo("42");
  }

  @Test
  void getDetailReturnsEmptyExposureWhenAlertsViewNull() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var terminalId = SellerTerminalId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var drawChannelId = DrawChannelId.of(UUID.randomUUID());

    var drawSummary =
        new DrawSummary(
            drawId, tenantId, LocalDate.of(2026, 7, 19), DrawStatus.OPEN, null, null, null, null,
            null, null, drawChannelId, "GA_EVE", "Georgia Eve", LocalTime.NOON, "UTC", true, null,
            null, null, null, null, true, null);

    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any())).thenReturn(drawSummary).thenReturn(null);

    var service = new PosDrawsService(queryBus, null, null, null, null, null);
    var result = service.getDetail(ctxWithTerminal(tenantId, terminalId), drawId);

    assertThat(result.exposure().active()).isTrue();
    assertThat(result.exposure().limitConfigured()).isFalse();
    assertThat(result.exposure().alerts()).isEmpty();
    assertThat(result.topSelections()).isEmpty();
  }

  @Test
  void getDetailMarksExposureInactiveWhenDrawClosed() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var terminalId = SellerTerminalId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var drawChannelId = DrawChannelId.of(UUID.randomUUID());

    var drawSummary =
        new DrawSummary(
            drawId, tenantId, LocalDate.of(2026, 7, 19), DrawStatus.CLOSED, null, null, null, null,
            null, null, drawChannelId, "GA_EVE", "Georgia Eve", LocalTime.NOON, "UTC", true, null,
            null, null, null, null, true, null);

    var alertItem =
        new ExposureAlertItemView(
            BetType.MATCH_1_2D,
            "07",
            BigDecimal.valueOf(200),
            5,
            BigDecimal.valueOf(500),
            BigDecimal.valueOf(0.4));
    var alertsView =
        new ExposureAlertsOverviewView(tenantId, drawId, "terminal", List.of(alertItem));

    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any())).thenReturn(drawSummary).thenReturn(alertsView);

    var service = new PosDrawsService(queryBus, null, null, null, null, null);
    var result = service.getDetail(ctxWithTerminal(tenantId, terminalId), drawId);

    assertThat(result.exposure().active()).isFalse();
    assertThat(result.exposure().limitConfigured()).isTrue();
    assertThat(result.exposure().alerts()).hasSize(1);
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

  private static TchRequestContext ctxWithTerminal(
      TenantId tenantId, SellerTerminalId terminalId) {
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
        terminalId,
        Set.of(),
        Set.of(),
        null);
  }

  private static final class FailingQueryBus implements QueryBus {
    @Override
    public <R> R ask(Query<R> query) {
      throw new AssertionError("Draw query must not be called when tenant business day is closed");
    }
  }

  private record FixedTimeProvider(Instant fixedNow) implements TchTimeProvider {
    @Override
    public Instant now() {
      return fixedNow;
    }

    @Override
    public LocalDate today(ZoneId zoneId) {
      return fixedNow.atZone(zoneId).toLocalDate();
    }

    @Override
    public ZonedDateTime nowAt(ZoneId zoneId) {
      return fixedNow.atZone(zoneId);
    }

    @Override
    public Clock clock() {
      return Clock.fixed(fixedNow, ZoneId.of("UTC"));
    }
  }

  private static final class EmptyDrawChannelCatalog implements DrawChannelCatalog {
    @Override
    public List<ChannelGamesView> listChannelGames(TenantId tenantId) {
      return List.of();
    }

    @Override
    public List<DrawChannelSummaryView> listAll(TenantId tenantId, Boolean activeOnly) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<DrawChannelView> listAllFull(TenantId tenantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long countActiveChannels(TenantId tenantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DrawChannelView> findById(TenantId tenantId, DrawChannelId id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DrawChannelView> findByTenantAndCode(TenantId tenantId, String code) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<DrawChannelGameView> listGamesByChannel(
        TenantId tenantId, DrawChannelId channelId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<DrawChannelCalendarRow> listCalendarRows(
        TenantId tenantId, Boolean activeOnly, Boolean enabledOnly) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TchPage<DrawChannelView> search(
        DrawChannelSearchCriteria criteria, TchPageRequest pageReq) {
      throw new UnsupportedOperationException();
    }
  }
}
