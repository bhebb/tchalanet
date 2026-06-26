package com.tchalanet.server.platform.notification.internal.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.job.lifecycle.JobLifecycleEvent;
import com.tchalanet.server.common.job.lifecycle.JobLifecycleStatus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.internal.rule.BatchAlertNotificationRule;
import com.tchalanet.server.platform.notification.internal.rule.PayoutNotificationRule;
import com.tchalanet.server.platform.notification.internal.service.NotificationService;
import com.tchalanet.server.platform.notification.internal.service.NotificationTriggerService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

class NotificationDomainEventRouterTest {

  @Test
  void duplicateEventSkipsNotificationCreation() {
    var notificationService = mock(NotificationService.class);
    var triggerService = triggerService(true);
    var processedEvents = provider(false);
    var router =
        new NotificationDomainEventRouter(
            List.of(new PayoutNotificationRule()),
            notificationService,
            triggerService,
            new JsonUtils(JsonMapper.builder().build()),
            processedEvents);

    router.on(new PayoutRequestedEvent(UUID.randomUUID(), TenantId.of(UUID.randomUUID())));

    verify(notificationService, never()).createNotification(any(CreateNotificationRequest.class));
  }

  @Test
  void newEventCreatesNotification() {
    var notificationService = mock(NotificationService.class);
    var triggerService = triggerService(true);
    var processedEvents = provider(true);
    var router =
        new NotificationDomainEventRouter(
            List.of(new PayoutNotificationRule()),
            notificationService,
            triggerService,
            new JsonUtils(JsonMapper.builder().build()),
            processedEvents);

    router.on(new PayoutRequestedEvent(UUID.randomUUID(), TenantId.of(UUID.randomUUID())));

    verify(notificationService).createNotification(any(CreateNotificationRequest.class));
  }

  @Test
  void failedJobCreatesPlatformSystemNotification() {
    var notificationService = mock(NotificationService.class);
    var triggerService = triggerService(true);
    var processedEvents = provider(true);
    var router =
        new NotificationDomainEventRouter(
            List.of(new BatchAlertNotificationRule()),
            notificationService,
            triggerService,
            new JsonUtils(JsonMapper.builder().build()),
            processedEvents);
    var eventId = UUID.randomUUID();

    router.on(new JobLifecycleEvent(
        EventId.of(eventId),
        Instant.parse("2026-06-26T12:00:00Z"),
        null,
        "req-1",
        "results:external:fetch",
        JobLifecycleStatus.FAILED,
        "IllegalStateException",
        "Provider NY_EVE failed",
        Map.of("providerKey", "NY_EVE")));

    var captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
    verify(processedEvents.getIfAvailable())
        .markProcessedIfAbsent(eq("notification.ops.job_lifecycle"), eq(eventId));
    verify(triggerService)
        .markTriggeredIfAbsent(
            eq("notification.ops.job_lifecycle"),
            eq("JobLifecycleEvent"),
            eq(eventId.toString()),
            eq(null));
    verify(notificationService).createNotification(captor.capture());

    var request = captor.getValue();
    org.assertj.core.api.Assertions.assertThat(request.tenantId()).isNull();
    org.assertj.core.api.Assertions.assertThat(request.audienceType())
        .isEqualTo(NotificationAudienceType.PLATFORM_ADMINS);
    org.assertj.core.api.Assertions.assertThat(request.severity())
        .isEqualTo(NotificationSeverity.CRITICAL);
    org.assertj.core.api.Assertions.assertThat(request.kind())
        .isEqualTo(NotificationKind.SYSTEM_ERROR);
    org.assertj.core.api.Assertions.assertThat(request.titleKey())
        .isEqualTo("notification.system.ops.job_failed");
  }

  private static ObjectProvider<ProcessedEventPort> provider(boolean markProcessedResult) {
    var port = mock(ProcessedEventPort.class);
    when(port.markProcessedIfAbsent(any(), any())).thenReturn(markProcessedResult);

    @SuppressWarnings("unchecked")
    ObjectProvider<ProcessedEventPort> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(port);
    return provider;
  }

  private static NotificationTriggerService triggerService(boolean markTriggeredResult) {
    var service = mock(NotificationTriggerService.class);
    when(service.markTriggeredIfAbsent(any(), any(), any(), any())).thenReturn(markTriggeredResult);
    return service;
  }

  private record PayoutRequestedEvent(UUID eventId, TenantId tenantId) {}
}
