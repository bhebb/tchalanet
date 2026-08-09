package com.tchalanet.server.core.draw.internal.infra.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.internal.infra.cache.DrawCacheEvictor;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawEventListenerTest {

  @Test
  void resultAppliedEvictsCacheWithoutTenantAdminNotification() {
    var processed = mock(ProcessedEventPort.class);
    when(processed.markProcessedIfAbsent(anyString(), any(UUID.class))).thenReturn(true);
    var evictor = mock(DrawCacheEvictor.class);
    var notificationApi = mock(NotificationApi.class);

    var tenantId = TenantId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var drawResultId = DrawResultId.of(UUID.randomUUID());
    var listener = new DrawEventListener(processed, evictor);

    listener.onDrawResultApplied(
        new DrawResultAppliedEvent(
            EventId.of(UUID.randomUUID()),
            Instant.parse("2026-08-08T20:00:00Z"),
            tenantId,
            drawId,
            LocalDate.of(2026, 8, 8),
            ResultSlotId.of(UUID.randomUUID()),
            drawResultId,
            DrawChannelId.of(UUID.randomUUID())));

    verify(evictor).evictAll();
    verify(notificationApi, never()).createNotification(any(CreateNotificationRequest.class));
  }
}
