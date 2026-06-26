package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.types.id.NotificationId;
import java.time.Instant;
import java.util.Optional;

public interface NotificationWriter {
  Optional<Notification> findByDedupeKey(String dedupeKey);

  Notification save(Notification notification);

  int expire(Instant now);
}
