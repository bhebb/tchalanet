package com.tchalanet.server.core.draw.internal.infra.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.json.utils.JsonUtils;
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
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DrawEventListenerTest {

  @Test
  void resultAppliedCreatesTenantAdminNotification() {
    var processed = mock(ProcessedEventPort.class);
    when(processed.markProcessedIfAbsent(anyString(), any(UUID.class))).thenReturn(true);
    var evictor = mock(DrawCacheEvictor.class);
    var notificationApi = mock(NotificationApi.class);
    var jsonUtils = mock(JsonUtils.class);
    when(jsonUtils.toJsonNode(any())).thenReturn(JsonUtils.emptyObject());

    var tenantId = TenantId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var drawResultId = DrawResultId.of(UUID.randomUUID());
    var listener = new DrawEventListener(processed, evictor, notificationApi, jsonUtils);

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
    var captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
    verify(notificationApi).createNotification(captor.capture());

    var request = captor.getValue();
    assertThat(request.tenantId()).isEqualTo(tenantId);
    assertThat(request.audienceType()).isEqualTo(NotificationAudienceType.TENANT_ADMINS);
    assertThat(request.category()).isEqualTo(NotificationCategory.DRAW);
    assertThat(request.channels()).containsExactly(NotificationChannel.WEB);
    assertThat(request.dedupeKey())
        .isEqualTo(
            "draw.result.applied:%s:%s:TENANT_ADMINS"
                .formatted(drawId.value(), drawResultId.value()));
    assertThat(request.actionUrl()).isEqualTo("/app/admin/draws/" + drawId.value());
    assertThat(request.translations()).containsOnlyKeys("fr", "en", "ht");
    assertThat(request.translations().get("ht").title()).isEqualTo("Rezilta aplike");
  }
}
