package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.types.id.NotificationId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationWriterPort {
  Optional<Notification> findByDedupeKey(String dedupeKey);

  Notification save(Notification notification);

  void markRead(NotificationId id, Instant readAt);

  void markRead(List<NotificationId> ids, Instant readAt);

  void archive(NotificationId id, Instant archivedAt);

  void archive(List<NotificationId> ids, Instant archivedAt);

  int expire(Instant now);
}
