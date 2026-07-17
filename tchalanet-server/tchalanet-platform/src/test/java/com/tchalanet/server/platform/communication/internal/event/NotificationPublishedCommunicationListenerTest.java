package com.tchalanet.server.platform.communication.internal.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationPublicationId;
import com.tchalanet.server.platform.notification.api.model.NotificationPublishedEvent;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationPublishedCommunicationListenerTest {

  @Test
  void slackNotificationIsEnqueuedOnceWithStableCorrelationKey() {
    var communicationApi = mock(CommunicationApi.class);
    var listener = new NotificationPublishedCommunicationListener(communicationApi, List.of());
    var notificationId = NotificationId.of(UUID.randomUUID());
    var publicationId = NotificationPublicationId.of(UUID.randomUUID());

    listener.on(
        new NotificationPublishedEvent(
            UUID.randomUUID(),
            notificationId,
            publicationId,
            null,
            NotificationAudienceType.PLATFORM_ADMINS,
            Set.of(),
            NotificationSeverity.WARNING,
            NotificationKind.ACTION_REQUIRED,
            NotificationCategory.RESULT,
            "Résultat manuel requis",
            "Provider=MN, slot=MN_EVE",
            JsonUtils.emptyObject(),
            "/app/platform/ops/draw-results",
            Set.of(NotificationDeliveryChannel.SLACK),
            Instant.parse("2026-07-16T22:00:00Z")));

    var captor = ArgumentCaptor.forClass(SendOutboundMessageRequest.class);
    verify(communicationApi).enqueue(captor.capture());

    var request = captor.getValue();
    assertThat(request.channel()).isEqualTo(CommunicationChannel.SLACK_INTERNAL);
    assertThat(request.recipient().channelKey()).isEqualTo("notifications");
    assertThat(request.correlationKey())
        .isEqualTo("notification:%s:SLACK_INTERNAL:notifications".formatted(publicationId.value()));
    assertThat(request.subject()).isEqualTo("Résultat manuel requis");
    assertThat(request.body()).contains("Provider=MN");
  }
}
