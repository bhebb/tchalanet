package com.tchalanet.server.platform.notification.internal.event;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.internal.rule.NotificationIntent;
import com.tchalanet.server.platform.notification.internal.rule.NotificationRule;
import com.tchalanet.server.platform.notification.internal.service.NotificationService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDomainEventRouter {

  private final List<NotificationRule> rules;
  private final NotificationService notificationService;
  private final JsonUtils jsonUtils;
  private final ObjectProvider<ProcessedEventPort> processedEvents;

  @EventListener
  public void on(Object event) {
    for (var rule : rules) {
      if (!rule.supports(event)) {
        continue;
      }

      rule.map(event).forEach(intent -> createIfNotProcessed(rule, intent));
    }
  }

  private void createIfNotProcessed(NotificationRule rule, NotificationIntent intent) {
    if (!markProcessed(rule, intent)) {
      return;
    }

    notificationService.createNotification(new CreateNotificationRequest(
        intent.tenantId(),
        intent.sourceType(),
        intent.sourceEventId() == null ? null : intent.sourceEventId().toString(),
        intent.correlationKey(),
        intent.audienceType(),
        intent.audienceValue(),
        intent.severity(),
        intent.kind(),
        intent.category(),
        intent.templateKey(),
        intent.templateKey(),
        intent.title(),
        intent.message(),
        jsonUtils.toJsonNode(intent.variables()),
        null,
        null,
        null,
        Set.of(NotificationChannel.WEB)));
  }

  private boolean markProcessed(NotificationRule rule, NotificationIntent intent) {
    if (intent.sourceEventId() == null) {
      return true;
    }

    try {
      var port = processedEvents.getIfAvailable();
      return port == null || port.markProcessedIfAbsent(rule.handlerKey(), intent.sourceEventId());
    } catch (RuntimeException ex) {
      log.debug("Processed-event check unavailable for notification rule {}; using notification dedupe key",
          rule.handlerKey(), ex);
      return true;
    }
  }
}
