package com.tchalanet.server.core.draw.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotStatsView;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawApplyPort;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultProjection;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultsCriteria;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResult;
import com.tchalanet.server.core.drawresult.internal.infra.config.DrawResultsProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ApplyExternalResultsWindowCommandHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
  private static final ResultSlotId SLOT_ID =
      ResultSlotId.of(UUID.fromString("60000000-0000-0000-0000-000000000001"));
  private static final DrawId DRAW_ID =
      DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001"));
  private static final DrawChannelId DRAW_CHANNEL_ID =
      DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001"));
  private static final DrawResultId DRAW_RESULT_ID =
      DrawResultId.of(UUID.fromString("50000000-0000-0000-0000-000000000001"));
  private static final Instant NOW = Instant.parse("2026-01-02T22:00:00Z");

  @Test
  void publishesDrawResultAppliedEventWhenExternalResultIsAttachedToDraw() {
    var publisher = new CapturingPublisher();
    var handler =
        new ApplyExternalResultsWindowCommandHandler(
            new SingleSlotCatalog(),
            new SingleDrawResultReader(),
            new UpdatedDrawApplyPort(),
            new DrawResultsProperties(),
            Clock.fixed(NOW, ZoneOffset.UTC),
            publisher,
            () -> UUID.fromString("01000000-0000-0000-0000-000000000001"),
            new JsonUtils(JsonMapper.builder().build()));

    var result =
        handler.handle(
            new ApplyExternalResultsWindowCommand(
                TENANT_ID, LocalDate.of(2026, 1, 2), 0, List.of("MIDDAY"), false, false, 10, null));

    assertThat(result.inserted()).isEqualTo(1);
    assertThat(publisher.events()).hasSize(1);
    assertThat(publisher.events().getFirst())
        .isInstanceOfSatisfying(
            DrawResultAppliedEvent.class,
            event -> {
              assertThat(event.tenantId()).isEqualTo(TENANT_ID);
              assertThat(event.drawId()).isEqualTo(DRAW_ID);
              assertThat(event.drawResultId()).isEqualTo(DRAW_RESULT_ID);
              assertThat(event.resultSlotId()).isEqualTo(SLOT_ID);
              assertThat(event.drawChannelId()).isEqualTo(DRAW_CHANNEL_ID);
            });
  }

  private static final class SingleSlotCatalog implements ResultSlotCatalog {
    @Override
    public List<ResultSlotView> listActive() {
      return List.of(slot());
    }

    @Override
    public Optional<ResultSlotView> findByKey(String slotKey) {
      return Optional.of(slot());
    }

    @Override
    public ResultSlotView requireByKey(String slotKey) {
      return slot();
    }

    @Override
    public Optional<ResultSlotView> findById(ResultSlotId id) {
      return Optional.of(slot());
    }

    @Override
    public boolean existsLive(ResultSlotId id) {
      return true;
    }

    @Override
    public ResultSlotStatsView stats() {
      throw new UnsupportedOperationException();
    }

    private static ResultSlotView slot() {
      return new ResultSlotView(
          SLOT_ID,
          "MIDDAY",
          "TEST",
          ZoneId.of("UTC"),
          LocalTime.of(22, 0),
          "MON,TUE,WED,THU,FRI,SAT,SUN",
          true,
          null,
          null,
          "slot.midday");
    }
  }

  private static final class SingleDrawResultReader implements DrawResultReaderPort {
    @Override
    public DrawResult getById(DrawResultId id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DrawResultView> findViewById(DrawResultId id) {
      return Optional.empty();
    }

    @Override
    public Optional<DrawResultView> findViewBySlotKeyAndOccurredAt(
        String slotKey, Instant occurredAt) {
      return Optional.empty();
    }

    @Override
    public Optional<DrawResultProjection> findProjectionById(DrawResultId id) {
      return Optional.empty();
    }

    @Override
    public Optional<DrawResultProjection> findProjectionBySlotKeyAndOccurredAt(
        String slotKey, Instant occurredAt) {
      return Optional.empty();
    }

    @Override
    public Optional<DrawResultId> findByResultSlotIdAndOccurredAt(
        ResultSlotId resultSlotId, Instant occurredAt) {
      return Optional.of(DRAW_RESULT_ID);
    }

    @Override
    public Optional<DrawResultId> findByResultSlotIdAndResultDate(
        ResultSlotId resultSlotId, LocalDate resultDate) {
      return Optional.empty();
    }

    @Override
    public TchPage<DrawResultView> findViewsByCriteria(DrawResultsCriteria criteria) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DrawResultView> findByDrawId(DrawId drawId) {
      return Optional.empty();
    }

    @Override
    public boolean existsUsableExternalResult(ResultSlotId resultSlotId, LocalDate resultDate) {
      return true;
    }
  }

  private static final class UpdatedDrawApplyPort implements DrawApplyPort {
    @Override
    public ApplyResult attachResultBySlot(
        TenantId tenantId,
        LocalDate drawDate,
        ResultSlotId resultSlotId,
        DrawResultId drawResultId,
        Instant now) {
      return ApplyResult.updated(List.of(new AppliedDraw(DRAW_ID, DRAW_CHANNEL_ID)));
    }
  }

  private static final class CapturingPublisher implements DomainEventPublisher {
    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
      events.add(event);
    }

    @Override
    public void publish(Collection<? extends DomainEvent> events) {
      this.events.addAll(events);
    }

    private List<DomainEvent> events() {
      return events;
    }
  }
}
