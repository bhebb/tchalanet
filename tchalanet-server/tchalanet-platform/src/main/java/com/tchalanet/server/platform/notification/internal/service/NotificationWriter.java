package com.tchalanet.server.platform.notification.internal.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface NotificationWriter {
  Optional<Notification> findByDedupeKey(String dedupeKey);

  Notification save(Notification notification);

  int expire(Instant now);

  int expireByDedupeKeys(Collection<String> dedupeKeys, Instant now);
}
