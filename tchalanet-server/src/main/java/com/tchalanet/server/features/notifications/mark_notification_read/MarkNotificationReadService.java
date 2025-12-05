package com.tchalanet.server.features.notifications.mark_notification_read;

import com.tchalanet.server.features.notifications.shared.NotificationEntity;
import com.tchalanet.server.features.notifications.shared.NotificationJpaRepository;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Use case pour marquer une notification comme lue.
 */
@Service
public class MarkNotificationReadService {

  private final NotificationJpaRepository repo;

  public MarkNotificationReadService(NotificationJpaRepository repo) {
    this.repo = repo;
  }

  public void handle(UUID userId, UUID notificationId) {
    NotificationEntity entity =
        repo
            .findById(notificationId)
            .orElseThrow(() -> new NoSuchElementException("Notification not found"));

    if (!entity.getUserId().equals(userId)) {
      throw new NoSuchElementException("Notification not for the user found");
    }

    if (!entity.isRead()) {
      entity.setRead(true);
      entity.setReadAt(Instant.now());
      repo.save(entity);
    }
  }
}

