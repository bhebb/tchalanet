package com.tchalanet.server.core.draw.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.ChannelGamesView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelCalendarRow;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelGameView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSearchCriteria;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.query.ExistingDrawKey;
import com.tchalanet.server.core.draw.api.query.NewDrawRow;
import com.tchalanet.server.core.draw.api.query.OpenableDrawRow;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.ResultSlotCalendarReaderPort;
import com.tchalanet.server.core.draw.internal.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.internal.domain.model.Draw;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GenerateDrawsForRangeCommandHandlerTest {

  @Test
  void providerOpenDateIsGeneratedEvenWhenTenantBusinessCalendarMayBeClosedElsewhere() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var slotId = ResultSlotId.of(UUID.randomUUID());
    var drawDate = LocalDate.of(2026, 1, 2);
    var lifecycle = new FakeDrawLifecyclePort();
    var handler =
        new GenerateDrawsForRangeCommandHandler(
            new FakeDrawChannelCatalog(slotId),
            lifecycle,
            new FakeResultSlotCalendarReaderPort(slotId, drawDate.plusDays(1)),
            () -> UUID.randomUUID(),
            new FixedTimeProvider(Instant.parse("2026-01-01T12:00:00Z")));

    var result =
        handler.handle(
            new GenerateDrawsForRangeCommand(tenantId, drawDate, drawDate, false, false, null));

    assertThat(result.created()).isEqualTo(1);
    assertThat(result.skippedProviderClosed()).isZero();
    assertThat(lifecycle.rows).hasSize(1);
    assertThat(lifecycle.rows.getFirst().tenantId()).isEqualTo(tenantId);
    assertThat(lifecycle.rows.getFirst().drawDate()).isEqualTo(drawDate);
    assertThat(lifecycle.rows.getFirst().status()).isEqualTo("SCHEDULED");
  }

  @Test
  void providerClosedDateIsNotGenerated() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var slotId = ResultSlotId.of(UUID.randomUUID());
    var drawDate = LocalDate.of(2026, 1, 2);
    var lifecycle = new FakeDrawLifecyclePort();
    var handler =
        new GenerateDrawsForRangeCommandHandler(
            new FakeDrawChannelCatalog(slotId),
            lifecycle,
            new FakeResultSlotCalendarReaderPort(slotId, drawDate),
            () -> UUID.randomUUID(),
            new FixedTimeProvider(Instant.parse("2026-01-01T12:00:00Z")));

    var result =
        handler.handle(
            new GenerateDrawsForRangeCommand(tenantId, drawDate, drawDate, false, false, null));

    assertThat(result.created()).isZero();
    assertThat(result.skippedProviderClosed()).isEqualTo(1);
    assertThat(lifecycle.rows).isEmpty();
  }

  private record FakeResultSlotCalendarReaderPort(ResultSlotId closedSlotId, LocalDate closedDate)
      implements ResultSlotCalendarReaderPort {
    @Override
    public Set<LocalDate> findUnavailableDates(
        ResultSlotId resultSlotId, LocalDate fromInclusive, LocalDate toInclusive) {
      return closedSlotId.equals(resultSlotId) && closedDate.equals(fromInclusive)
          ? Set.of(closedDate)
          : Set.of();
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

  private static final class FakeDrawLifecyclePort implements DrawLifecyclePort {
    private final List<NewDrawRow> rows = new ArrayList<>();

    @Override
    public int bulkInsert(List<NewDrawRow> rows) {
      this.rows.addAll(rows);
      return rows.size();
    }

    @Override
    public Set<ExistingDrawKey> findExistingKeys(TenantId tenantId, LocalDate from, LocalDate to) {
      return Set.of();
    }

    @Override
    public List<OpenableDrawRow> findOpenable(
        Instant now, int limit, int openHorizonHours, int openLagHours) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int bulkOpen(List<DrawId> drawIds, Instant now) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int bulkCancelScheduled(
        List<DrawId> drawIds, String reasonCode, String reasonLabel, Instant now) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<DueToCloseRow> findDueToClose(Instant now, int limit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int bulkClose(List<DrawId> drawIds, Instant now) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Draw save(Draw draw) {
      throw new UnsupportedOperationException();
    }
  }

  private record FakeDrawChannelCatalog(ResultSlotId slotId) implements DrawChannelCatalog {
    @Override
    public List<DrawChannelCalendarRow> listCalendarRows(
        TenantId tenantId, Boolean activeOnly, Boolean enabledOnly) {
      return List.of(
          new DrawChannelCalendarRow(
              DrawChannelId.of(UUID.randomUUID()),
              null,
              "HT_NY_MID",
              "America/New_York",
              LocalTime.NOON,
              LocalTime.of(8, 0),
              300,
              "MON-SUN",
              slotId,
              "SYSTEM",
              true,
              true,
              1,
              null));
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
    public List<ChannelGamesView> listChannelGames(TenantId tenantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TchPage<DrawChannelView> search(
        DrawChannelSearchCriteria criteria, TchPageRequest pageReq) {
      throw new UnsupportedOperationException();
    }
  }
}
