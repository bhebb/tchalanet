package com.tchalanet.server.core.draw.internal.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ListCashierNextDrawsQueryHandlerTest {

  private static final Instant NOW = Instant.parse("2026-07-08T14:00:00Z");

  @Test
  void requestsOpenDrawsBeforePagingAndMapping() {
    var reader = new CapturingReader(nextDraw());
    var handler = new ListCashierNextDrawsQueryHandler(reader, fixedTimeProvider());

    var result = handler.handle(new ListCashierNextDrawsQuery(48, 20));

    assertThat(reader.criteria.status()).isEqualTo(DrawStatus.OPEN);
    assertThat(reader.criteria.lookaheadHours()).isEqualTo(48);
    assertThat(reader.criteria.limitPerChannel()).isEqualTo(20);
    assertThat(result)
        .singleElement()
        .satisfies(draw -> assertThat(draw.status()).isEqualTo("OPEN"));
  }

  private static DrawSummary nextDraw() {
    return new DrawSummary(
        DrawId.of(UUID.fromString("10000000-0000-0000-0000-000000000001")),
        TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001")),
        LocalDate.of(2026, 7, 9),
        DrawStatus.OPEN,
        NOW.plusSeconds(3600),
        NOW,
        null,
        NOW.plusSeconds(1800),
        null,
        null,
        DrawChannelId.of(UUID.fromString("30000000-0000-0000-0000-000000000001")),
        "NATIONAL",
        "National",
        LocalTime.NOON,
        "America/Port-au-Prince",
        true,
        ResultSlotId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")),
        "MIDDAY",
        "internal",
        "America/Port-au-Prince",
        LocalTime.NOON,
        true,
        null);
  }

  private static TchTimeProvider fixedTimeProvider() {
    return new TchTimeProvider() {
      @Override
      public Instant now() {
        return NOW;
      }

      @Override
      public LocalDate today(ZoneId zoneId) {
        return NOW.atZone(zoneId).toLocalDate();
      }

      @Override
      public ZonedDateTime nowAt(ZoneId zoneId) {
        return NOW.atZone(zoneId);
      }

      @Override
      public Clock clock() {
        return Clock.fixed(NOW, ZoneId.of("UTC"));
      }
    };
  }

  private static final class CapturingReader implements DrawSummaryReaderPort {
    private final DrawSummary draw;
    private DrawSearchCriteria criteria;

    private CapturingReader(DrawSummary draw) {
      this.draw = draw;
    }

    @Override
    public Optional<DrawSummary> findById(DrawId drawId) {
      return Optional.empty();
    }

    @Override
    public DrawSummary getById(DrawId drawId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TchPage<DrawSummary> findByCriteria(DrawSearchCriteria criteria, Pageable pageable) {
      return emptyPage();
    }

    @Override
    public TchPage<DrawSummary> listNext(DrawSearchCriteria criteria, Pageable pageable) {
      this.criteria = criteria;
      return TchPage.of(List.of(draw), 0, 20, 1, 1, true, false, false);
    }

    @Override
    public TchPage<DrawSummary> listLatestWithResults(
        DrawSearchCriteria criteria, Pageable pageable) {
      return emptyPage();
    }

    private TchPage<DrawSummary> emptyPage() {
      return TchPage.of(List.of(), 0, 20, 0, 0, true, false, false);
    }
  }
}
