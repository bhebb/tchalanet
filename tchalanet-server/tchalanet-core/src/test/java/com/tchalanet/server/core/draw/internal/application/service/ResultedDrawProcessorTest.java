package com.tchalanet.server.core.draw.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.event.DrawSettlementAttentionEvent;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.DrawResultSummary;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.internal.domain.model.Draw;
import com.tchalanet.server.core.draw.internal.infra.config.DrawProperties;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import com.tchalanet.server.core.sales.api.command.result.RecordDrawTicketsResultResult;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResultedDrawProcessorTest {

  private static final Instant NOW = Instant.parse("2026-08-03T15:00:00Z");

  @Test
  void publishesAttentionAfterTheConfiguredThreshold() {
    var fixture = fixture(NOW.minusSeconds(31 * 60));
    var processor = fixture.processor();

    assertThat(processor.process(fixture.drawId())).isFalse();

    var captor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(fixture.events()).publish(captor.capture());
    assertThat(captor.getValue()).isInstanceOf(DrawSettlementAttentionEvent.class);
    var event = (DrawSettlementAttentionEvent) captor.getValue();
    assertThat(event.drawId()).isEqualTo(fixture.drawId());
    assertThat(event.pendingTickets()).isEqualTo(3L);
    assertThat(event.failedTickets()).isEqualTo(1);
  }

  @Test
  void keepsProcessingSilentBeforeTheConfiguredThreshold() {
    var fixture = fixture(NOW.minusSeconds(29 * 60));

    assertThat(fixture.processor().process(fixture.drawId())).isFalse();

    verify(fixture.events(), never()).publish(any(DomainEvent.class));
  }

  @Test
  void settlesOnlyAfterAllTicketsAreTerminal() {
    var fixture = fixture(NOW.minusSeconds(31 * 60), commandBusWithCompleteOutcome());

    assertThat(fixture.processor().process(fixture.drawId())).isTrue();

    verify(fixture.events(), never()).publish(any(DomainEvent.class));
  }

  private static Fixture fixture(Instant resultedAt) {
    return fixture(resultedAt, commandBusWithIncompleteOutcome());
  }

  private static Fixture fixture(Instant resultedAt, CommandBus commandBus) {
    var drawId = DrawId.of(UUID.randomUUID());
    var tenantId = TenantId.of(UUID.randomUUID());
    var drawChannelId = DrawChannelId.of(UUID.randomUUID());
    var resultSlotId = ResultSlotId.of(UUID.randomUUID());
    var drawResultId = DrawResultId.of(UUID.randomUUID());
    var drawDate = LocalDate.of(2026, 8, 3);
    var draw =
        new Draw(
            drawId,
            tenantId,
            drawChannelId,
            drawDate,
            NOW.minusSeconds(3600),
            NOW.minusSeconds(7200),
            DrawStatus.RESULTED,
            drawResultId,
            NOW.minusSeconds(7200),
            NOW.minusSeconds(3600),
            resultedAt,
            null,
            null,
            null,
            null,
            DrawSource.EXTERNAL,
            null,
            null,
            false,
            true);
    var summary =
        new DrawSummary(
            drawId,
            tenantId,
            drawDate,
            DrawStatus.RESULTED,
            NOW.minusSeconds(7200),
            NOW.minusSeconds(7200),
            NOW.minusSeconds(3600),
            NOW.minusSeconds(3600),
            resultedAt,
            null,
            drawChannelId,
            "NY_EVE",
            "New York Evening",
            LocalTime.of(20, 0),
            "America/New_York",
            true,
            resultSlotId,
            "NY_EVE",
            "NY",
            "America/New_York",
            LocalTime.of(20, 0),
            true,
            new DrawResultSummary(
                drawResultId,
                DrawResultStatus.CONFIRMED,
                resultedAt,
                resultedAt,
                "hash",
                Map.of()));
    var lookup = mock(DrawLookupPort.class);
    when(lookup.findById(drawId)).thenReturn(Optional.of(draw));
    var summaries = mock(DrawSummaryReaderPort.class);
    when(summaries.getById(drawId)).thenReturn(summary);
    var events = mock(DomainEventPublisher.class);
    var queryBus = mock(QueryBus.class);
    doReturn(false).when(queryBus).ask(any());
    var properties = new DrawProperties();
    properties.getScheduler().getProcessing().getSettle().setAttentionAfterMinutes(30);
    var processor =
        new ResultedDrawProcessor(
            lookup,
            summaries,
            commandBus,
            queryBus,
            properties,
            events,
            Clock.fixed(NOW, ZoneOffset.UTC));
    return new Fixture(drawId, events, processor);
  }

  private static CommandBus commandBusWithIncompleteOutcome() {
    return new CommandBus() {
      @Override
      @SuppressWarnings("unchecked")
      public <R> R execute(Command<R> command) {
        return (R) new RecordDrawTicketsResultResult(3, 0, 3, 3, 1);
      }
    };
  }

  private static CommandBus commandBusWithCompleteOutcome() {
    return new CommandBus() {
      @Override
      @SuppressWarnings("unchecked")
      public <R> R execute(Command<R> command) {
        return (R) new RecordDrawTicketsResultResult(3, 3, 0, 0, 0);
      }
    };
  }

  private record Fixture(
      DrawId drawId, DomainEventPublisher events, ResultedDrawProcessor processor) {}
}
