package com.tchalanet.server.features.pos.draws;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.DrawChannelDisplayFormatter;
import com.tchalanet.server.catalog.drawchannel.api.model.ChannelGamesView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelCalendarRow;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelGameView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSearchCriteria;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.tenant.api.model.TenantBusinessDayView;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PosDrawsServiceTest {

  @Test
  void tenantClosedBusinessDayReturnsNoAvailableDrawsWithoutQueryingDraws() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var now = Instant.parse("2026-01-02T14:00:00Z");
    var service =
        new PosDrawsService(
            new FailingQueryBus(),
            new EmptyDrawChannelCatalog(),
            new DrawChannelDisplayFormatter(),
            (id, date) ->
                new TenantBusinessDayView(id, date, false, "TENANT_CLOSED", "Tenant closed"),
            new FixedTimeProvider(now));

    var result = service.listAvailable(ctx(tenantId), 24, 20);

    assertThat(result).isEmpty();
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
