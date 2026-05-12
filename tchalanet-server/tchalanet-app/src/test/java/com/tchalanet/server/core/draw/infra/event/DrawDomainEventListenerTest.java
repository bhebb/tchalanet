package com.tchalanet.server.core.draw.infra.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.idempotency.event.ProcessedEventPort;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.infra.cache.DrawCacheEvictor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawDomainEventListenerTest {

  @Test
  void onDrawResultAppliedEvictsCache() {
    var processed = new InMemoryProcessedEventPort();
    var cacheEvictor = new RecordingCacheEvictor();
    var listener = new DrawEventListener(processed, cacheEvictor);
    var event = event(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    listener.onDrawResultApplied(event);

    assertThat(cacheEvictor.evictAllCalled).isTrue();
    assertThat(processed.markProcessedCalls).isEqualTo(1);
  }

  @Test
  void onDrawResultAppliedSkipsIfAlreadyProcessed() {
    var processed = new InMemoryProcessedEventPort();
    var cacheEvictor = new RecordingCacheEvictor();
    var listener = new DrawEventListener(processed, cacheEvictor);
    var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    processed.processed.add(eventId);
    var event = event(eventId);

    listener.onDrawResultApplied(event);

    assertThat(cacheEvictor.evictAllCalled).isFalse();
    assertThat(processed.markProcessedCalls).isEqualTo(0);
  }

  private static DrawResultAppliedEvent event(UUID eventId) {
    return new DrawResultAppliedEvent(
        EventId.of(eventId),
        Instant.parse("2026-04-28T12:00:00Z"),
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")),
        DrawId.of(UUID.fromString("00000000-0000-0000-0000-000000000020")),
        LocalDate.of(2026, 4, 28),
        ResultSlotId.of(UUID.fromString("00000000-0000-0000-0000-000000000030")),
        DrawResultId.of(UUID.fromString("00000000-0000-0000-0000-000000000040")),
        DrawChannelId.of(UUID.fromString("00000000-0000-0000-0000-000000000050")));
  }

  private static class InMemoryProcessedEventPort implements ProcessedEventPort {
    private final Set<UUID> processed = new HashSet<>();
    int markProcessedCalls;

    @Override
    public boolean alreadyProcessed(String handlerKey, UUID eventId) {
      return processed.contains(eventId);
    }

    @Override
    public void markProcessed(String handlerKey, UUID eventId) {
      markProcessedIfAbsent(handlerKey, eventId);
    }

    @Override
    public boolean markProcessedIfAbsent(String handlerKey, UUID eventId) {
      if (processed.contains(eventId)) {
        return false;
      }
      processed.add(eventId);
      markProcessedCalls++;
      return true;
    }
  }

  private static class RecordingCacheEvictor extends DrawCacheEvictor {
    boolean evictAllCalled = false;

    public RecordingCacheEvictor() {
      super(null);
    }

    @Override
    public void evictAll() {
      evictAllCalled = true;
    }
  }
}
