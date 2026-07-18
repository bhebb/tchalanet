package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.platform.notification.api.model.NotificationPublishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
class NotificationPublishedRealtimeListener {

  private final NotificationRealtimeStreamService streams;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(NotificationPublishedEvent event) {
    streams.publish(event);
  }
}
