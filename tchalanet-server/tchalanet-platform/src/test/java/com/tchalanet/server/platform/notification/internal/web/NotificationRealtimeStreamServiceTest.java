package com.tchalanet.server.platform.notification.internal.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationPublicationId;
import com.tchalanet.server.platform.notification.api.model.NotificationPublishedEvent;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationRealtimeStreamServiceTest {

  @Test
  void routesTenantAudienceOnlyToMatchingAdminTenant() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var matching =
        subscription(NotificationRealtimeScope.ADMIN, UUID.randomUUID(), tenantId.value());
    var otherTenant =
        subscription(NotificationRealtimeScope.ADMIN, UUID.randomUUID(), UUID.randomUUID());

    var event = event(NotificationAudienceType.TENANT_ADMINS, tenantId);

    assertThat(NotificationRealtimeStreamService.matches(matching, event)).isTrue();
    assertThat(NotificationRealtimeStreamService.matches(otherTenant, event)).isFalse();
  }

  @Test
  void routesPlatformAudienceOnlyToPlatformStreams() {
    var platform = subscription(NotificationRealtimeScope.PLATFORM, UUID.randomUUID(), null);
    var admin = subscription(NotificationRealtimeScope.ADMIN, UUID.randomUUID(), UUID.randomUUID());

    var event = event(NotificationAudienceType.PLATFORM_ADMINS, null);

    assertThat(NotificationRealtimeStreamService.matches(platform, event)).isTrue();
    assertThat(NotificationRealtimeStreamService.matches(admin, event)).isFalse();
  }

  private static NotificationRealtimeStreamService.Subscription subscription(
      NotificationRealtimeScope scope, UUID userId, UUID tenantId) {
    return new NotificationRealtimeStreamService.Subscription(
        scope, userId, tenantId, new SseEmitter());
  }

  private static NotificationPublishedEvent event(
      NotificationAudienceType audience, TenantId tenantId) {
    return new NotificationPublishedEvent(
        UUID.randomUUID(),
        NotificationId.of(UUID.randomUUID()),
        NotificationPublicationId.of(UUID.randomUUID()),
        tenantId,
        audience,
        Set.of(),
        NotificationSeverity.INFO,
        NotificationKind.INFO,
        NotificationCategory.SYSTEM,
        "title",
        "message",
        null,
        null,
        Set.of(),
        Instant.now());
  }
}
