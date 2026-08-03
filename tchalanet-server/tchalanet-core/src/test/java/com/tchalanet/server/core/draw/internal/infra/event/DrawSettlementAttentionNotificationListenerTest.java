package com.tchalanet.server.core.draw.internal.infra.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.draw.api.event.DrawSettledEvent;
import com.tchalanet.server.core.draw.api.event.DrawSettlementAttentionEvent;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.ExpireNotificationsByDedupeKeysRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class DrawSettlementAttentionNotificationListenerTest {

  private static final Instant NOW = Instant.parse("2026-08-03T15:00:00Z");

  @Test
  void createsOnePlatformActionRequiredNotification() {
    var processedEvents = mock(ProcessedEventPort.class);
    when(processedEvents.markProcessedIfAbsent(any(), any())).thenReturn(true);
    var notifications = mock(NotificationApi.class);
    var event = attentionEvent();
    var listener = listener(processedEvents, notifications);

    listener.onSettlementAttention(event);

    var captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
    verify(processedEvents)
        .markProcessedIfAbsent(eq("draw.settlement.attention"), eq(event.eventId().value()));
    verify(notifications).createNotification(captor.capture());

    var request = captor.getValue();
    assertThat(request.tenantId()).isNull();
    assertThat(request.audienceType()).isEqualTo(NotificationAudienceType.PLATFORM_ADMINS);
    assertThat(request.severity()).isEqualTo(NotificationSeverity.ERROR);
    assertThat(request.kind()).isEqualTo(NotificationKind.ACTION_REQUIRED);
    assertThat(request.channels())
        .containsExactlyInAnyOrder(NotificationChannel.WEB, NotificationChannel.SLACK);
    assertThat(request.dedupeKey())
        .isEqualTo(
            DrawSettlementAttentionNotificationListener.dedupeKey(
                event.drawId().value().toString(), event.drawResultId().value().toString()));
    assertThat(request.actionUrl())
        .isEqualTo(
            "/app/platform/ops/draws/" + event.tenantId().value() + "/" + event.drawId().value());
    assertThat(request.payload().get("tenantId").asText())
        .isEqualTo(event.tenantId().value().toString());
    assertThat(request.payload().get("pendingTickets").asLong()).isEqualTo(7L);
    assertThat(request.payload().get("failedTickets").asInt()).isEqualTo(2);
  }

  @Test
  void duplicateAttentionEventDoesNotCreateAnotherNotification() {
    var processedEvents = mock(ProcessedEventPort.class);
    when(processedEvents.markProcessedIfAbsent(any(), any())).thenReturn(false);
    var notifications = mock(NotificationApi.class);

    listener(processedEvents, notifications).onSettlementAttention(attentionEvent());

    verify(notifications, never()).createNotification(any());
  }

  @Test
  void settlementExpiresTheMatchingAttentionNotice() {
    var notifications = mock(NotificationApi.class);
    var event = attentionEvent();
    var listener = listener(mock(ProcessedEventPort.class), notifications);

    listener.onDrawSettled(
        new DrawSettledEvent(
            EventId.of(UUID.randomUUID()),
            NOW,
            event.tenantId(),
            event.drawId(),
            event.drawChannelId(),
            event.resultSlotId(),
            event.drawResultId(),
            event.drawDate(),
            NOW.minusSeconds(60)));

    var captor = ArgumentCaptor.forClass(ExpireNotificationsByDedupeKeysRequest.class);
    verify(notifications).expireByDedupeKeys(captor.capture());
    assertThat(captor.getValue().dedupeKeys())
        .containsExactly(
            DrawSettlementAttentionNotificationListener.dedupeKey(
                event.drawId().value().toString(), event.drawResultId().value().toString()));
  }

  @Test
  void correctionExpiresAttentionForTheSupersededResult() {
    var notifications = mock(NotificationApi.class);
    var event = attentionEvent();
    var listener = listener(mock(ProcessedEventPort.class), notifications);

    listener.onDrawResultCorrected(
        new DrawResultCorrectedEvent(
            EventId.of(UUID.randomUUID()),
            NOW,
            event.tenantId(),
            event.drawId(),
            event.drawDate(),
            event.resultSlotId(),
            event.drawResultId(),
            DrawResultId.of(UUID.randomUUID()),
            event.drawChannelId(),
            "provider correction"));

    var captor = ArgumentCaptor.forClass(ExpireNotificationsByDedupeKeysRequest.class);
    verify(notifications).expireByDedupeKeys(captor.capture());
    assertThat(captor.getValue().dedupeKeys())
        .containsExactly(
            DrawSettlementAttentionNotificationListener.dedupeKey(
                event.drawId().value().toString(), event.drawResultId().value().toString()));
  }

  private static DrawSettlementAttentionNotificationListener listener(
      ProcessedEventPort processedEvents, NotificationApi notifications) {
    return new DrawSettlementAttentionNotificationListener(
        processedEvents,
        notifications,
        new JsonUtils(JsonMapper.builder().build()),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static DrawSettlementAttentionEvent attentionEvent() {
    return new DrawSettlementAttentionEvent(
        EventId.of(UUID.randomUUID()),
        NOW,
        TenantId.of(UUID.randomUUID()),
        DrawId.of(UUID.randomUUID()),
        DrawChannelId.of(UUID.randomUUID()),
        ResultSlotId.of(UUID.randomUUID()),
        DrawResultId.of(UUID.randomUUID()),
        LocalDate.of(2026, 8, 3),
        NOW.minusSeconds(1800),
        7,
        2);
  }
}
