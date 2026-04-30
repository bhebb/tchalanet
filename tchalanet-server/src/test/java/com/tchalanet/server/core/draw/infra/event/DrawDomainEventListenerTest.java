package com.tchalanet.server.core.draw.infra.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.idempotency.event.ProcessedEventPort;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.drawresult.domain.event.DrawResultIngestedEvent;
import com.tchalanet.server.core.drawresult.infra.cache.DrawResultCacheEvictor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawDomainEventListenerTest {

  @Test
  void onDrawResultIngestedIsReplaySafe() {
    var processed = new InMemoryProcessedEventPort();
    var evictor = new RecordingCacheEvictor();
    var listener = new DrawDomainEventListener(processed, evictor);
    var event = event(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    listener.onDrawResultIngested(event);
    listener.onDrawResultIngested(event);

    assertThat(evictor.evictAllCalls).isEqualTo(1);
    assertThat(processed.markProcessedCalls).isEqualTo(1);
  }

  private static DrawResultIngestedEvent event(UUID eventId) {
    return new DrawResultIngestedEvent(
        EventId.of(eventId),
        Instant.parse("2026-04-28T12:00:00Z"),
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000101")),
        DrawId.of(UUID.fromString("00000000-0000-0000-0000-000000000102")),
        DrawChannelId.of(UUID.fromString("00000000-0000-0000-0000-000000000103")),
        ResultSlotId.of(UUID.fromString("00000000-0000-0000-0000-000000000104")),
        DrawResultId.of(UUID.fromString("00000000-0000-0000-0000-000000000105")),
        Instant.parse("2026-04-28T11:55:00Z"),
        LocalDate.parse("2026-04-28"));
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

  private static class RecordingCacheEvictor extends DrawResultCacheEvictor {
    int evictAllCalls;

    RecordingCacheEvictor() {
      super(null);
    }

    @Override
    public void evictAll() {
      evictAllCalls++;
    }
  }
}
