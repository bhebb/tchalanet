package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationPublishedEvent;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Sends content-free refresh signals; notification content remains available only through REST. */
@Service
@Slf4j
class NotificationRealtimeStreamService {

  private static final long NO_TIMEOUT = 0L;
  private final Set<Subscription> subscriptions = ConcurrentHashMap.newKeySet();

  SseEmitter connect(NotificationRealtimeScope scope, UUID userId, TenantId tenantId) {
    var emitter = new SseEmitter(NO_TIMEOUT);
    var subscription =
        new Subscription(scope, userId, tenantId == null ? null : tenantId.value(), emitter);
    subscriptions.add(subscription);
    emitter.onCompletion(() -> subscriptions.remove(subscription));
    emitter.onTimeout(() -> subscriptions.remove(subscription));
    emitter.onError(error -> subscriptions.remove(subscription));
    try {
      emitter.send(SseEmitter.event().name("connected").data("{}", MediaType.APPLICATION_JSON));
    } catch (IOException | IllegalStateException error) {
      subscriptions.remove(subscription);
      emitter.completeWithError(error);
    }
    return emitter;
  }

  void publish(NotificationPublishedEvent event) {
    subscriptions.stream()
        .filter(subscription -> matches(subscription, event))
        .forEach(this::sendChange);
  }

  static boolean matches(Subscription subscription, NotificationPublishedEvent event) {
    var audience = event.audienceType();
    if (audience == NotificationAudienceType.ALL_APP_USERS) {
      return true;
    }
    if (audience == NotificationAudienceType.PLATFORM_ADMINS) {
      return subscription.scope() == NotificationRealtimeScope.PLATFORM;
    }
    if (audience == NotificationAudienceType.SPECIFIC_ACTORS) {
      return event.targets().stream()
          .anyMatch(
              target ->
                  target.actorType() == NotificationActorType.APP_USER
                      && target.actorId().equals(subscription.userId()));
    }
    return subscription.scope() == NotificationRealtimeScope.ADMIN
        && event.tenantId() != null
        && event.tenantId().value().equals(subscription.tenantId());
  }

  private void sendChange(Subscription subscription) {
    try {
      subscription
          .emitter()
          .send(
              SseEmitter.event()
                  .name("notification-change")
                  .data("{}", MediaType.APPLICATION_JSON));
    } catch (IOException | IllegalStateException error) {
      subscriptions.remove(subscription);
      log.debug("notification.realtime.stream.closed", error);
    }
  }

  record Subscription(
      NotificationRealtimeScope scope, UUID userId, UUID tenantId, SseEmitter emitter) {}
}
